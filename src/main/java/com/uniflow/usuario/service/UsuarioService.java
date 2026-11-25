package com.uniflow.usuario.service;

import com.uniflow.auth.security.CurrentUserService;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.ConstraintViolationUtils;
import com.uniflow.usuario.dto.UpdateProfileRequest;
import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.mapper.UsuarioMapper;
import com.uniflow.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Servico de aplicacao responsavel por consulta e atualizacao do perfil do usuario autenticado ({@code MOD-02}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

	private static final String EMAIL_CONSTRAINT_NAME = "uk_usuarios_email";
	private static final String EMAIL_DUPLICADO_MESSAGE = "Ja existe um usuario cadastrado com este e-mail.";

	private final CurrentUserService currentUserService;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioMapper usuarioMapper;

	/**
	 * Consulta o perfil do usuario autenticado identificado exclusivamente via {@code SecurityContext}.
	 *
	 * @return dados publicos atuais da conta autenticada
	 */
	@Transactional(readOnly = true)
	public UsuarioResponse getProfile() {
		Usuario usuario = currentUserService.getCurrentUser();
		return usuarioMapper.toResponse(usuario);
	}

	/**
	 * Atualiza os dados pessoais permitidos do usuario autenticado ({@code nome} e/ou {@code email}),
	 * garantindo unicidade case-insensitive do e-mail e isolamento total por conta.
	 *
	 * @param request payload validado com os campos a atualizar
	 * @return dados publicos atualizados do usuario autenticado
	 */
	@Transactional
	public UsuarioResponse updateProfile(UpdateProfileRequest request) {
		Objects.requireNonNull(request, "request nao deve ser nulo");

		Usuario usuario = currentUserService.getCurrentUser();

		if (request.nome() != null) {
			usuario.setNome(request.nome());
		}

		if (request.email() != null && !request.email().equals(usuario.getEmail())) {
			if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
				throw new ConflictException(EMAIL_DUPLICADO_MESSAGE);
			}
			usuario.setEmail(request.email());
		}

		try {
			Usuario atualizado = usuarioRepository.saveAndFlush(usuario);
			log.info("Perfil atualizado com sucesso para usuarioId={}", atualizado.getId());
			return usuarioMapper.toResponse(atualizado);
		} catch (DataIntegrityViolationException ex) {
			if (ConstraintViolationUtils.containsConstraint(ex, EMAIL_CONSTRAINT_NAME)) {
				throw new ConflictException(EMAIL_DUPLICADO_MESSAGE);
			}
			throw ex;
		}
	}
}
