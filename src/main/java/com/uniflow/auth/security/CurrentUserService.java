package com.uniflow.auth.security;

import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Componente central para extracao segura da identidade do usuario autenticado
 * exclusivamente a partir do {@link SecurityContextHolder} ({@code BE-027}, {@code RNF-003}).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

	private static final String UNAUTHENTICATED_MESSAGE = "Autenticacao necessaria ou credenciais invalidas.";

	private final UsuarioRepository usuarioRepository;

	/**
	 * Retorna o principal {@link AuthenticatedUser} presente no {@code SecurityContext} da requisicao atual.
	 *
	 * @return principal autenticado com status apto
	 */
	public AuthenticatedUser getCurrentAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
			throw new AuthenticationFailedException(UNAUTHENTICATED_MESSAGE);
		}
		if (!authenticatedUser.isEnabled()) {
			throw new AuthenticationFailedException(UNAUTHENTICATED_MESSAGE);
		}
		return authenticatedUser;
	}

	/**
	 * Retorna o identificador {@link UUID} do usuario autenticado sem executar nova consulta ao banco.
	 *
	 * @return {@code UUID} do usuario autenticado no {@code SecurityContext}
	 */
	public UUID getCurrentUserId() {
		return getCurrentAuthenticatedUser().id();
	}

	/**
	 * Carrega a entidade {@link Usuario} atualizada do banco para o usuario autenticado no {@code SecurityContext},
	 * revalidando que a conta permanece apta para autenticacao.
	 *
	 * @return entidade {@link Usuario} do proprietario autenticado
	 */
	@Transactional(readOnly = true)
	public Usuario getCurrentUser() {
		UUID userId = getCurrentUserId();
		Usuario usuario = usuarioRepository.findById(userId)
				.orElseThrow(() -> new AuthenticationFailedException(UNAUTHENTICATED_MESSAGE));
		if (!usuario.podeAutenticar()) {
			throw new AuthenticationFailedException(UNAUTHENTICATED_MESSAGE);
		}
		return usuario;
	}
}
