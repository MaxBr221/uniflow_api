package com.uniflow.auth.service;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.ChangePasswordRequest;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.LogoutRequest;
import com.uniflow.auth.dto.RefreshTokenRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.mapper.AuthMapper;
import com.uniflow.auth.security.CurrentUserService;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.BusinessRuleException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.ConstraintViolationUtils;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Servico de aplicacao responsavel pelos fluxos de autenticacao e cadastro de usuarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String EMAIL_CONSTRAINT_NAME = "uk_usuarios_email";
	private static final String EMAIL_DUPLICADO_MESSAGE = "Ja existe um usuario cadastrado com este e-mail.";
	private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciais invalidas.";
	private static final String INVALID_CURRENT_PASSWORD_MESSAGE = "Senha atual invalida.";
	private static final String SAME_PASSWORD_MESSAGE = "A nova senha deve ser diferente da senha atual.";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthMapper authMapper;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final CurrentUserService currentUserService;

	/**
	 * Realiza o cadastro de uma nova conta de estudante garantindo unicidade de e-mail
	 * case-insensitive e armazenamento seguro da senha com BCrypt.
	 *
	 * @param request payload de cadastro validado
	 * @return dados publicos do usuario cadastrado, sem {@code senhaHash}
	 */
	@Transactional
	public AuthUserResponse register(RegisterRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		String normalizedNome = request.nome().trim();
		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

		if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ConflictException(EMAIL_DUPLICADO_MESSAGE);
		}

		String senhaHash = passwordEncoder.encode(request.senha());
		Usuario novoUsuario = new Usuario(normalizedNome, normalizedEmail, senhaHash);

		try {
			Usuario salvo = usuarioRepository.saveAndFlush(novoUsuario);
			log.info("Novo usuario cadastrado com id={} e status={}", salvo.getId(), salvo.getStatus());
			return authMapper.toUserResponse(salvo);
		} catch (DataIntegrityViolationException ex) {
			if (isEmailUniqueConstraintViolation(ex)) {
				throw new ConflictException(EMAIL_DUPLICADO_MESSAGE);
			}
			throw ex;
		}
	}

	/**
	 * Autentica um usuario existente por e-mail e senha, emitindo access token JWT e refresh token opaco.
	 *
	 * @param request credenciais validadas de login
	 * @return tokens de autenticacao e dados publicos do usuario autenticado
	 */
	@Transactional
	public AuthTokenResponse login(LoginRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE));

		if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash()) || !usuario.podeAutenticar()) {
			throw new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE);
		}

		String accessToken = jwtService.generateAccessToken(usuario);
		RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(usuario);
		AuthUserResponse userResponse = authMapper.toUserResponse(usuario);
		log.info("Autenticacao realizada com sucesso para usuarioId={}", usuario.getId());
		return AuthTokenResponse.bearer(
				accessToken,
				refreshToken.rawToken(),
				jwtService.getAccessExpirationSeconds(),
				userResponse
		);
	}

	/**
	 * Renova a sessao validando e rotacionando o refresh token opaco recebido.
	 * <p>
	 * Mantem {@code noRollbackFor = AuthenticationFailedException.class} para preservar em banco a
	 * revogacao em cascata quando detectada reutilizacao de token ja rotacionado ou revogado.
	 *
	 * @param request payload validado contendo o refresh token atual
	 * @return novo par de access token JWT e refresh token rotacionado
	 */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	public AuthTokenResponse refresh(RefreshTokenRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(request.refreshToken());
		String newAccessToken = jwtService.generateAccessToken(rotated.usuario());
		AuthUserResponse userResponse = authMapper.toUserResponse(rotated.usuario());
		log.info("Sessao renovada com rotacao de refresh token para usuarioId={}", rotated.usuario().getId());
		return AuthTokenResponse.bearer(
				newAccessToken,
				rotated.newRawToken(),
				jwtService.getAccessExpirationSeconds(),
				userResponse
		);
	}

	/**
	 * Encerra a sessao do usuario autenticado (identificado exclusivamente via {@code SecurityContext})
	 * revogando o refresh token informado ou, caso nenhum token especifico seja enviado, todos os refresh tokens ativos da conta.
	 *
	 * @param request payload opcional com refresh token especifico
	 */
	@Transactional
	public void logout(LogoutRequest request) {
		UUID authenticatedUserId = currentUserService.getCurrentUserId();

		int revokedTokens = (request != null && request.refreshToken() != null)
				? refreshTokenService.revokeTokenForUser(authenticatedUserId, request.refreshToken())
				: refreshTokenService.revokeAllForUser(authenticatedUserId);

		log.info("Logout concluido para usuarioId={} (tokensRevogados={})", authenticatedUserId, revokedTokens);
	}

	/**
	 * Consulta os dados publicos atualizados do usuario atualmente autenticado no {@code SecurityContext}.
	 *
	 * @return dados publicos do usuario autenticado sem exposicao de {@code senhaHash}
	 */
	@Transactional(readOnly = true)
	public AuthUserResponse getCurrentUser() {
		Usuario usuario = currentUserService.getCurrentUser();
		return authMapper.toUserResponse(usuario);
	}

	/**
	 * Altera a senha do usuario autenticado no {@code SecurityContext}, validando a senha atual,
	 * impedindo reutilizacao da senha vigente, aplicando hash BCrypt na nova senha e revogando
	 * todos os refresh tokens ativos da conta.
	 *
	 * @param request payload validado contendo {@code senhaAtual} e {@code novaSenha}
	 */
	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		Usuario usuario = currentUserService.getCurrentUser();
		if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
			throw new BusinessRuleException(INVALID_CURRENT_PASSWORD_MESSAGE);
		}
		if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
			throw new BusinessRuleException(SAME_PASSWORD_MESSAGE);
		}

		usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
		usuarioRepository.saveAndFlush(usuario);
		int revokedTokens = refreshTokenService.revokeAllForUser(usuario.getId());
		log.info("Senha alterada com sucesso para usuarioId={} (refreshTokensRevogados={})", usuario.getId(), revokedTokens);
	}

	private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException ex) {
		return ConstraintViolationUtils.containsConstraint(ex, EMAIL_CONSTRAINT_NAME);
	}
}
