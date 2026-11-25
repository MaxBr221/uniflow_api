package com.uniflow.usuario.service;

import com.uniflow.auth.security.CurrentUserService;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.usuario.dto.UpdateProfileRequest;
import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.mapper.UsuarioMapper;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private CurrentUserService currentUserService;

	@Mock
	private UsuarioRepository usuarioRepository;

	private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		usuarioService = new UsuarioService(currentUserService, usuarioRepository, new UsuarioMapper());
	}

	@Test
	void shouldReturnCurrentAuthenticatedUserProfile() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);

		UsuarioResponse response = usuarioService.getProfile();

		assertThat(response.id()).isEqualTo(usuario.getId());
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		verifyNoMoreInteractions(usuarioRepository);
	}

	@Test
	void shouldUpdateNomeOnlyWithoutEmailUniquenessCheck() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

		UsuarioResponse response = usuarioService.updateProfile(new UpdateProfileRequest("  Ana Maria Silva  ", null));

		assertThat(response.nome()).isEqualTo("Ana Maria Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
		verify(usuarioRepository).saveAndFlush(usuario);
	}

	@Test
	void shouldAllowKeepingSameNormalizedEmailWithoutSelfConflict() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

		UsuarioResponse response = usuarioService.updateProfile(
				new UpdateProfileRequest("Ana Atualizada", "  ANA.SILVA@UniFlow.EDU  ")
		);

		assertThat(response.nome()).isEqualTo("Ana Atualizada");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
		verify(usuarioRepository).saveAndFlush(usuario);
	}

	@Test
	void shouldUpdateNewNormalizedEmailAfterUniquenessCheck() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(usuarioRepository.existsByEmailIgnoreCase("ana.nova@uniflow.edu")).thenReturn(false);
		when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

		UsuarioResponse response = usuarioService.updateProfile(
				new UpdateProfileRequest(null, "  ANA.NOVA@UniFlow.EDU ")
		);

		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.nova@uniflow.edu");
		verify(usuarioRepository).existsByEmailIgnoreCase("ana.nova@uniflow.edu");
		verify(usuarioRepository).saveAndFlush(usuario);
	}

	@Test
	void shouldThrowConflictExceptionWhenNewEmailAlreadyBelongsToAnotherUser() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(usuarioRepository.existsByEmailIgnoreCase("outro@uniflow.edu")).thenReturn(true);

		assertThatThrownBy(() -> usuarioService.updateProfile(new UpdateProfileRequest(null, "outro@uniflow.edu")))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("e-mail");

		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldTranslateRaceConditionUniqueEmailConstraintViolationToConflictException() {
		Usuario usuario = createUsuario("Ana Silva", "ana.silva@uniflow.edu");
		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(usuarioRepository.existsByEmailIgnoreCase("concorrente@uniflow.edu")).thenReturn(false);

		var hibernateException = new ConstraintViolationException(
				"duplicate key value violates unique constraint \"uk_usuarios_email\"",
				new SQLException("duplicate key", "23505"),
				"uk_usuarios_email"
		);
		when(usuarioRepository.saveAndFlush(usuario))
				.thenThrow(new DataIntegrityViolationException("constraint", hibernateException));

		assertThatThrownBy(() -> usuarioService.updateProfile(new UpdateProfileRequest(null, "concorrente@uniflow.edu")))
				.isInstanceOf(ConflictException.class);
	}

	private static Usuario createUsuario(String nome, String email) {
		Instant now = Instant.parse("2026-11-25T15:00:00Z");
		Usuario usuario = new Usuario(nome, email, "$2a$10$hashSecreto");
		usuario.setId(UUID.randomUUID());
		usuario.setCreatedAt(now);
		usuario.setUpdatedAt(now);
		return usuario;
	}
}
