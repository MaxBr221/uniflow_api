package com.uniflow.auth.service;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.mapper.AuthMapper;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

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

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthMapper authMapper;
	private final JwtService jwtService;

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
	 * Autentica um usuario existente por e-mail e senha, emitindo access token JWT.
	 *
	 * @param request credenciais validadas de login
	 * @return token JWT e dados publicos do usuario autenticado
	 */
	@Transactional(readOnly = true)
	public AuthTokenResponse login(LoginRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE));

		if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash()) || !usuario.podeAutenticar()) {
			throw new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE);
		}

		String accessToken = jwtService.generateAccessToken(usuario);
		AuthUserResponse userResponse = authMapper.toUserResponse(usuario);
		log.info("Autenticacao realizada com sucesso para usuarioId={}", usuario.getId());
		return AuthTokenResponse.bearer(
				accessToken,
				null,
				jwtService.getAccessExpirationSeconds(),
				userResponse
		);
	}

	private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException ex) {
		Throwable cause = ex;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException cve
					&& cve.getConstraintName() != null
					&& cve.getConstraintName().toLowerCase(Locale.ROOT).contains(EMAIL_CONSTRAINT_NAME)) {
				return true;
			}
			String message = cause.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains(EMAIL_CONSTRAINT_NAME)) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}
}
