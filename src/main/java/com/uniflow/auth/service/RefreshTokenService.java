package com.uniflow.auth.service;

import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.auth.security.OwnershipValidator;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servico responsavel pela emissao, persistencia por hash SHA-256, rotacao atomica e
 * bloqueio de reutilizacao de refresh tokens ({@code BE-024}, {@code RNF-002}).
 */
@Slf4j
@Service
public class RefreshTokenService {

	public static final long MIN_EXPIRATION_MS = 1_000L;
	public static final String TOKEN_PREFIX = "rft_";

	private static final int TOKEN_RANDOM_BYTES = 32;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token invalido ou expirado.";

	private final RefreshTokenRepository refreshTokenRepository;
	private final long refreshExpirationMs;
	private final Clock clock;

	@Autowired
	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
		this(refreshTokenRepository, refreshExpirationMs, Clock.systemUTC());
	}

	RefreshTokenService(RefreshTokenRepository refreshTokenRepository, long refreshExpirationMs, Clock clock) {
		if (refreshExpirationMs < MIN_EXPIRATION_MS) {
			throw new IllegalArgumentException(
					"Tempo de expiracao do refresh token deve ser de pelo menos " + MIN_EXPIRATION_MS + " ms."
			);
		}
		this.refreshTokenRepository = Objects.requireNonNull(
				refreshTokenRepository,
				"refreshTokenRepository nao deve ser nulo"
		);
		this.refreshExpirationMs = refreshExpirationMs;
		this.clock = Objects.requireNonNull(clock, "clock nao deve ser nulo");
	}

	/**
	 * Emite um novo refresh token opaco para usuario apto a autenticar, persistindo somente seu hash.
	 *
	 * @param usuario usuario autenticado
	 * @return par com token opaco para devolucao ao cliente e entidade persistida
	 */
	@Transactional
	public IssuedRefreshToken issue(Usuario usuario) {
		Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		Objects.requireNonNull(usuario.getId(), "usuario.id nao deve ser nulo para emissao de refresh token");
		if (!usuario.podeAutenticar()) {
			throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
		}
		return createAndSaveToken(usuario, clock.instant());
	}

	/**
	 * Valida e rotaciona um refresh token existente em operacao atomica.
	 * <p>
	 * Caso um refresh token ja revogado/rotacionado seja reutilizado, todos os refresh tokens ativos
	 * do usuario sao revogados imediatamente sem rollback antes de lancar {@link AuthenticationFailedException}.
	 *
	 * @param rawToken token opaco enviado pelo cliente
	 * @return usuario dono da sessao, novo refresh token opaco e entidade substituta
	 */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	public RotatedRefreshToken rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
		}

		String tokenHash = hashToken(rawToken.trim());
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE));

		Instant now = clock.instant();
		Usuario usuario = current.getUsuario();
		UUID usuarioId = usuario.getId();

		if (current.isRevoked()) {
			log.warn("Reutilizacao de refresh token ja revogado/rotacionado detectada para usuarioId={}", usuarioId);
			refreshTokenRepository.revokeAllActiveByUsuarioId(usuarioId, now);
			throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
		}

		if (current.isExpired(now)) {
			current.revoke(now);
			refreshTokenRepository.saveAndFlush(current);
			log.debug("Refresh token expirado invalidado para usuarioId={}", usuarioId);
			throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
		}

		if (!usuario.podeAutenticar()) {
			refreshTokenRepository.revokeAllActiveByUsuarioId(usuarioId, now);
			log.debug("Refresh token recusado para conta com status inapto: usuarioId={}", usuarioId);
			throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN_MESSAGE);
		}

		IssuedRefreshToken replacement = createAndSaveToken(usuario, now);
		current.markRotated(replacement.entity().getId(), now);
		refreshTokenRepository.saveAndFlush(current);

		log.debug("Refresh token rotacionado com sucesso para usuarioId={}", usuarioId);
		return new RotatedRefreshToken(usuario, replacement.rawToken(), replacement.entity());
	}

	/**
	 * Revoga um refresh token especifico pertencente ao usuario autenticado.
	 * <p>
	 * Bloqueia com {@link UnauthorizedOperationException} caso o token exista e pertenca a outro usuario;
	 * caso nao exista ou ja esteja revogado para o proprio usuario, opera de forma idempotente.
	 *
	 * @param usuarioId identificador do usuario autenticado
	 * @param rawToken  refresh token opaco a revogar
	 * @return quantidade de tokens efetivamente revogados ({@code 0} ou {@code 1})
	 */
	@Transactional
	public int revokeTokenForUser(UUID usuarioId, String rawToken) {
		Objects.requireNonNull(usuarioId, "usuarioId nao deve ser nulo");
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken nao deve ser nulo ou em branco.");
		}

		String tokenHash = hashToken(rawToken.trim());
		Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHashForUpdate(tokenHash);
		if (tokenOpt.isEmpty()) {
			return 0;
		}

		RefreshToken token = tokenOpt.get();
		OwnershipValidator.assertOwner(
				token.getUsuario().getId(),
				usuarioId,
				"Voce nao possui permissao para revogar este refresh token."
		);

		if (token.isRevoked()) {
			return 0;
		}

		token.revoke(clock.instant());
		refreshTokenRepository.saveAndFlush(token);
		return 1;
	}

	/**
	 * Revoga explicitamente todos os refresh tokens ativos de um usuario.
	 *
	 * @param usuarioId identificador do usuario proprietario
	 * @return quantidade de tokens ativos revogados
	 */
	@Transactional
	public int revokeAllForUser(UUID usuarioId) {
		Objects.requireNonNull(usuarioId, "usuarioId nao deve ser nulo");
		return refreshTokenRepository.revokeAllActiveByUsuarioId(usuarioId, clock.instant());
	}

	private IssuedRefreshToken createAndSaveToken(Usuario usuario, Instant issuedAt) {
		String rawToken = generateRawToken();
		String tokenHash = hashToken(rawToken);
		Instant expiresAt = issuedAt.plusMillis(refreshExpirationMs);

		RefreshToken entity = new RefreshToken(usuario, tokenHash, expiresAt, issuedAt);
		RefreshToken saved = refreshTokenRepository.saveAndFlush(entity);
		return new IssuedRefreshToken(rawToken, saved);
	}

	static String generateRawToken() {
		byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
		SECURE_RANDOM.nextBytes(randomBytes);
		return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	static String hashToken(String rawToken) {
		Objects.requireNonNull(rawToken, "rawToken nao deve ser nulo");
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", ex);
		}
	}

	/**
	 * Resultado de emissao de refresh token com o valor opaco exposto uma unica vez ao fluxo chamador.
	 */
	public record IssuedRefreshToken(String rawToken, RefreshToken entity) {
		@Override
		public String toString() {
			return "IssuedRefreshToken[rawToken=***REDACTED***, entity=" + entity + "]";
		}
	}

	/**
	 * Resultado de rotacao atomica contendo usuario validado, novo token opaco e entidade substituta.
	 */
	public record RotatedRefreshToken(Usuario usuario, String newRawToken, RefreshToken newEntity) {
		@Override
		public String toString() {
			return "RotatedRefreshToken[usuarioId=" + (usuario != null ? usuario.getId() : null)
					+ ", newRawToken=***REDACTED***, newEntity=" + newEntity + "]";
		}
	}
}
