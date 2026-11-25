package com.uniflow.auth.security;

import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	private CurrentUserService currentUserService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		currentUserService = new CurrentUserService(usuarioRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldReturnAuthenticatedPrincipalAndUserIdWithoutQueryingDatabase() {
		UUID userId = UUID.randomUUID();
		AuthenticatedUser principal = new AuthenticatedUser(userId, "ana@uniflow.edu", StatusUsuario.ATIVO);
		setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

		assertThat(currentUserService.getCurrentAuthenticatedUser()).isEqualTo(principal);
		assertThat(currentUserService.getCurrentUserId()).isEqualTo(userId);
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	void shouldLoadActiveUsuarioFromRepositoryInGetCurrentUser() {
		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);

		AuthenticatedUser principal = AuthenticatedUser.fromUsuario(usuario);
		setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

		Usuario loaded = currentUserService.getCurrentUser();

		assertThat(loaded).isSameAs(usuario);
		verify(usuarioRepository).findById(userId);
	}

	@Test
	void shouldRejectWhenSecurityContextIsEmptyAnonymousOrHasForeignPrincipal() {
		assertThatThrownBy(() -> currentUserService.getCurrentUserId())
				.isInstanceOf(AuthenticationFailedException.class);

		setAuthentication(new AnonymousAuthenticationToken(
				"anon-key",
				"anonymousUser",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
		));
		assertThatThrownBy(() -> currentUserService.getCurrentUserId())
				.isInstanceOf(AuthenticationFailedException.class);

		setAuthentication(new TestingAuthenticationToken("raw-string-principal", null, "ROLE_USER"));
		assertThatThrownBy(() -> currentUserService.getCurrentUserId())
				.isInstanceOf(AuthenticationFailedException.class);

		verifyNoInteractions(usuarioRepository);
	}

	@Test
	void shouldRejectWhenAuthenticatedPrincipalIsDisabledOrMissingOrBlockedInRepository() {
		UUID blockedPrincipalId = UUID.randomUUID();
		AuthenticatedUser blockedPrincipal = new AuthenticatedUser(
				blockedPrincipalId,
				"bloqueado@uniflow.edu",
				StatusUsuario.BLOQUEADO
		);
		setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
				blockedPrincipal,
				null,
				blockedPrincipal.getAuthorities()
		));

		assertThatThrownBy(() -> currentUserService.getCurrentAuthenticatedUser())
				.isInstanceOf(AuthenticationFailedException.class);
		verifyNoInteractions(usuarioRepository);

		UUID activeId = UUID.randomUUID();
		AuthenticatedUser activePrincipal = new AuthenticatedUser(activeId, "ana@uniflow.edu", StatusUsuario.ATIVO);
		setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
				activePrincipal,
				null,
				activePrincipal.getAuthorities()
		));

		when(usuarioRepository.findById(activeId)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> currentUserService.getCurrentUser())
				.isInstanceOf(AuthenticationFailedException.class);

		Usuario dbBlocked = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		dbBlocked.setId(activeId);
		dbBlocked.setStatus(StatusUsuario.INATIVO);
		when(usuarioRepository.findById(activeId)).thenReturn(Optional.of(dbBlocked));

		assertThatThrownBy(() -> currentUserService.getCurrentUser())
				.isInstanceOf(AuthenticationFailedException.class);
	}

	private static void setAuthentication(org.springframework.security.core.Authentication authentication) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}
}
