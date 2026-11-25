package com.uniflow.auth.service;

import com.uniflow.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Servico responsavel pela emissao e validacao de access tokens JWT ({@code RNF-002}).
 */
@Service
public class JwtService {

	public static final int MIN_SECRET_BYTES = 32;
	public static final long MIN_EXPIRATION_MS = 1_000L;
	public static final String CLAIM_EMAIL = "email";
	public static final String CLAIM_TOKEN_TYPE = "type";
	public static final String TOKEN_TYPE_ACCESS = "access";

	private final SecretKey signingKey;
	private final long accessExpirationMs;
	private final Clock clock;

	@Autowired
	public JwtService(
			@Value("${app.security.jwt.secret}") String secret,
			@Value("${app.security.jwt.access-expiration-ms:900000}") long accessExpirationMs) {
		this(secret, accessExpirationMs, Clock.systemUTC());
	}

	JwtService(String secret, long accessExpirationMs, Clock clock) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("Segredo JWT nao configurado.");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"Segredo JWT deve possuir ao menos " + MIN_SECRET_BYTES + " bytes em UTF-8."
			);
		}
		if (accessExpirationMs < MIN_EXPIRATION_MS) {
			throw new IllegalArgumentException(
					"Tempo de expiracao do access token deve ser de pelo menos " + MIN_EXPIRATION_MS + " ms."
			);
		}
		this.signingKey = Keys.hmacShaKeyFor(secretBytes);
		this.accessExpirationMs = accessExpirationMs;
		this.clock = Objects.requireNonNull(clock, "clock nao deve ser nulo");
	}

	/**
	 * Gera um access token JWT assinado contendo {@code sub}, {@code jti}, {@code iat} e {@code exp}.
	 *
	 * @param usuario usuario autenticado
	 * @return token JWT compacto assinado em HS256
	 */
	public String generateAccessToken(Usuario usuario) {
		Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		Objects.requireNonNull(usuario.getId(), "usuario.id nao deve ser nulo para emissao de JWT");

		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusMillis(accessExpirationMs);

		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(usuario.getId().toString())
				.claim(CLAIM_EMAIL, usuario.getEmail())
				.claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();
	}

	/**
	 * Valida assinatura, expiracao, tipo e claims obrigatorias do access token JWT.
	 *
	 * @param token token JWT em formato compacto
	 * @return claims validadas do payload JWT
	 */
	public Claims parseAndValidateAccessToken(String token) {
		if (token == null || token.isBlank()) {
			throw new JwtException("Token JWT ausente ou em branco.");
		}
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.require(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
				.clock(() -> Date.from(clock.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload();
		if (claims.getId() == null || claims.getId().isBlank()) {
			throw new JwtException("Token JWT sem claim jti obrigatoria.");
		}
		parseSubjectUuid(claims.getSubject());
		return claims;
	}

	public UUID extractUserId(String token) {
		return parseSubjectUuid(parseAndValidateAccessToken(token).getSubject());
	}

	public long getAccessExpirationSeconds() {
		return accessExpirationMs / 1_000L;
	}

	private static UUID parseSubjectUuid(String subject) {
		if (subject == null || subject.isBlank()) {
			throw new JwtException("Token JWT sem claim sub obrigatoria.");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException ex) {
			throw new JwtException("Claim sub do token JWT nao e um UUID valido.", ex);
		}
	}
}
