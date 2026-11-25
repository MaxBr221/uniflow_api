package com.uniflow.auth.service;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.mapper.AuthMapper;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(usuarioRepository, passwordEncoder, new AuthMapper(), jwtService);
	}

	@Test
	void shouldRegisterUsuarioWithNormalizedEmailEncodedPasswordAndPublicResponse() {
		RegisterRequest request = new RegisterRequest("  Ana Silva  ", "  ANA.SILVA@UniFlow.EDU  ", "UniFlow@2026");
		UUID idGerado = UUID.randomUUID();
		Instant agora = Instant.parse("2026-11-25T11:50:00Z");

		when(usuarioRepository.existsByEmailIgnoreCase("ana.silva@uniflow.edu")).thenReturn(false);
		when(passwordEncoder.encode("UniFlow@2026")).thenReturn("$2a$10$hashGeradoPeloBCrypt");
		when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
			Usuario entity = invocation.getArgument(0);
			entity.setId(idGerado);
			entity.setCreatedAt(agora);
			entity.setUpdatedAt(agora);
			return entity;
		});

		AuthUserResponse response = authService.register(request);

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).existsByEmailIgnoreCase("ana.silva@uniflow.edu");
		verify(passwordEncoder).encode("UniFlow@2026");
		verify(usuarioRepository).saveAndFlush(captor.capture());

		Usuario persistido = captor.getValue();
		assertThat(persistido.getNome()).isEqualTo("Ana Silva");
		assertThat(persistido.getEmail()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(persistido.getSenhaHash()).isEqualTo("$2a$10$hashGeradoPeloBCrypt");
		assertThat(persistido.getSenhaHash()).isNotEqualTo("UniFlow@2026");
		assertThat(persistido.getStatus()).isEqualTo(StatusUsuario.ATIVO);

		assertThat(response.id()).isEqualTo(idGerado);
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isEqualTo(agora);
		assertThat(response.updatedAt()).isEqualTo(agora);
	}

	@Test
	void shouldThrowConflictExceptionWhenNormalizedEmailAlreadyExists() {
		RegisterRequest request = new RegisterRequest("Ana Silva", "  ANA@UniFlow.EDU ", "UniFlow@2026");
		when(usuarioRepository.existsByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("e-mail");

		verifyNoInteractions(passwordEncoder);
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldTranslateEmailUniqueConstraintRaceConditionToConflictException() {
		RegisterRequest request = new RegisterRequest("Ana Silva", "ana@uniflow.edu", "UniFlow@2026");
		when(usuarioRepository.existsByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(false);
		when(passwordEncoder.encode("UniFlow@2026")).thenReturn("$2a$10$hashGeradoPeloBCrypt");

		var hibernateException = new org.hibernate.exception.ConstraintViolationException(
				"duplicate key value violates unique constraint \"uk_usuarios_email\"",
				new SQLException("ERROR: duplicate key value violates unique constraint \"uk_usuarios_email\"", "23505"),
				"uk_usuarios_email"
		);
		when(usuarioRepository.saveAndFlush(any(Usuario.class)))
				.thenThrow(new DataIntegrityViolationException("constraint violation", hibernateException));

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("e-mail");
	}

	@Test
	void shouldRethrowUnrelatedDataIntegrityViolation() {
		RegisterRequest request = new RegisterRequest("Ana Silva", "ana@uniflow.edu", "UniFlow@2026");
		when(usuarioRepository.existsByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(false);
		when(passwordEncoder.encode("UniFlow@2026")).thenReturn("$2a$10$hashGeradoPeloBCrypt");
		when(usuarioRepository.saveAndFlush(any(Usuario.class)))
				.thenThrow(new DataIntegrityViolationException("outra violacao qualquer"));

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldAuthenticateActiveUsuarioAndReturnBearerAccessToken() {
		UUID userId = UUID.randomUUID();
		Instant timestamp = Instant.parse("2026-11-25T12:00:00Z");
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(userId);
		usuario.setCreatedAt(timestamp);
		usuario.setUpdatedAt(timestamp);

		LoginRequest request = new LoginRequest("  ANA.SILVA@UniFlow.EDU  ", "UniFlow@2026");

		when(usuarioRepository.findByEmailIgnoreCase("ana.silva@uniflow.edu")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("UniFlow@2026", "$2a$10$hashSalvo")).thenReturn(true);
		when(jwtService.generateAccessToken(usuario)).thenReturn("jwt.access.gerado");
		when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);

		AuthTokenResponse response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("jwt.access.gerado");
		assertThat(response.refreshToken()).isNull();
		assertThat(response.expiresIn()).isEqualTo(900L);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.usuario().id()).isEqualTo(userId);
		assertThat(response.usuario().email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.usuario().status()).isEqualTo(StatusUsuario.ATIVO);
	}

	@Test
	void shouldThrowGenericAuthenticationFailedWhenEmailNotFoundOrPasswordWrong() {
		when(usuarioRepository.findByEmailIgnoreCase("desconhecido@uniflow.edu")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> authService.login(new LoginRequest("desconhecido@uniflow.edu", "UniFlow@2026")))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("Credenciais invalidas.");
		verifyNoInteractions(passwordEncoder, jwtService);

		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(UUID.randomUUID());
		when(usuarioRepository.findByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("SenhaErrada@2026", "$2a$10$hashSalvo")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("ana@uniflow.edu", "SenhaErrada@2026")))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("Credenciais invalidas.");
		verifyNoInteractions(jwtService);
	}

	@ParameterizedTest
	@EnumSource(value = StatusUsuario.class, names = {"INATIVO", "BLOQUEADO", "EXCLUIDO"})
	void shouldRejectLoginWithGenericErrorWhenUsuarioStatusDisallowsAuthentication(StatusUsuario statusBloqueado) {
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(UUID.randomUUID());
		usuario.setStatus(statusBloqueado);

		when(usuarioRepository.findByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("UniFlow@2026", "$2a$10$hashSalvo")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginRequest("ana@uniflow.edu", "UniFlow@2026")))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("Credenciais invalidas.");
		verifyNoInteractions(jwtService);
	}
}
