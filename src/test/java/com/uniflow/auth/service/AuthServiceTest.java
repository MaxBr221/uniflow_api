package com.uniflow.auth.service;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.ChangePasswordRequest;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.LogoutRequest;
import com.uniflow.auth.dto.RefreshTokenRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.mapper.AuthMapper;
import com.uniflow.auth.security.CurrentUserService;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.BusinessRuleException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
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

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private CurrentUserService currentUserService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				usuarioRepository,
				passwordEncoder,
				new AuthMapper(),
				jwtService,
				refreshTokenService,
				currentUserService
		);
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
		RefreshToken persistedRefresh = new RefreshToken(
				usuario,
				"a".repeat(64),
				timestamp.plusSeconds(604_800),
				timestamp
		);
		persistedRefresh.setId(UUID.randomUUID());
		when(refreshTokenService.issue(usuario))
				.thenReturn(new RefreshTokenService.IssuedRefreshToken("rft_refresh.gerado", persistedRefresh));

		AuthTokenResponse response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("jwt.access.gerado");
		assertThat(response.refreshToken()).isEqualTo("rft_refresh.gerado");
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
		verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);

		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(UUID.randomUUID());
		when(usuarioRepository.findByEmailIgnoreCase("ana@uniflow.edu")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("SenhaErrada@2026", "$2a$10$hashSalvo")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("ana@uniflow.edu", "SenhaErrada@2026")))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("Credenciais invalidas.");
		verifyNoInteractions(jwtService, refreshTokenService);
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
		verifyNoInteractions(jwtService, refreshTokenService);
	}

	@Test
	void shouldRotateRefreshTokenAndEmitNewAccessTokenOnSessionRefresh() {
		UUID userId = UUID.randomUUID();
		Instant timestamp = Instant.parse("2026-11-25T13:00:00Z");
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(userId);
		usuario.setCreatedAt(timestamp);
		usuario.setUpdatedAt(timestamp);

		RefreshToken replacementEntity = new RefreshToken(
				usuario,
				"b".repeat(64),
				timestamp.plusSeconds(604_800),
				timestamp
		);
		replacementEntity.setId(UUID.randomUUID());

		when(refreshTokenService.rotate("rft_token_atual"))
				.thenReturn(new RefreshTokenService.RotatedRefreshToken(usuario, "rft_novo_rotacionado", replacementEntity));
		when(jwtService.generateAccessToken(usuario)).thenReturn("jwt.access.renovado");
		when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);

		AuthTokenResponse response = authService.refresh(new RefreshTokenRequest("  rft_token_atual  "));

		assertThat(response.accessToken()).isEqualTo("jwt.access.renovado");
		assertThat(response.refreshToken()).isEqualTo("rft_novo_rotacionado");
		assertThat(response.expiresIn()).isEqualTo(900L);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.usuario().id()).isEqualTo(userId);
		assertThat(response.usuario().email()).isEqualTo("ana.silva@uniflow.edu");
	}

	@Test
	void shouldPropagateAuthenticationFailedWhenRefreshTokenRotationRejects() {
		when(refreshTokenService.rotate("rft_reutilizado"))
				.thenThrow(new AuthenticationFailedException("Refresh token invalido ou expirado."));

		assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("rft_reutilizado")))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("Refresh token invalido ou expirado.");
		verifyNoInteractions(jwtService);
	}

	@Test
	void shouldLogoutRevokingSpecificRefreshTokenWhenProvided() {
		UUID authenticatedUserId = UUID.randomUUID();
		when(currentUserService.getCurrentUserId()).thenReturn(authenticatedUserId);
		when(refreshTokenService.revokeTokenForUser(authenticatedUserId, "rft_sessao_atual")).thenReturn(1);

		authService.logout(new LogoutRequest("  rft_sessao_atual  "));

		verify(refreshTokenService).revokeTokenForUser(authenticatedUserId, "rft_sessao_atual");
		verify(refreshTokenService, never()).revokeAllForUser(any());
	}

	@Test
	void shouldLogoutRevokingAllActiveRefreshTokensWhenBodyOrTokenIsOmitted() {
		UUID authenticatedUserId = UUID.randomUUID();
		when(currentUserService.getCurrentUserId()).thenReturn(authenticatedUserId);
		when(refreshTokenService.revokeAllForUser(authenticatedUserId)).thenReturn(2);

		authService.logout(null);
		authService.logout(new LogoutRequest(null));

		verify(refreshTokenService, org.mockito.Mockito.times(2)).revokeAllForUser(authenticatedUserId);
		verify(refreshTokenService, never()).revokeTokenForUser(any(), any());
	}

	@Test
	void shouldPropagateUnauthorizedOperationWhenLogoutAttemptsToRevokeAnotherUsersToken() {
		UUID attackerId = UUID.randomUUID();
		when(currentUserService.getCurrentUserId()).thenReturn(attackerId);
		when(refreshTokenService.revokeTokenForUser(attackerId, "rft_alheio"))
				.thenThrow(new UnauthorizedOperationException("Voce nao possui permissao para revogar este refresh token."));

		assertThatThrownBy(() -> authService.logout(new LogoutRequest("rft_alheio")))
				.isInstanceOf(UnauthorizedOperationException.class);
	}

	@Test
	void shouldPropagateAuthenticationFailedWhenLogoutHasNoAuthenticatedUser() {
		when(currentUserService.getCurrentUserId())
				.thenThrow(new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas."));

		assertThatThrownBy(() -> authService.logout(null))
				.isInstanceOf(AuthenticationFailedException.class);

		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void shouldReturnMappedPublicUserForCurrentAuthenticatedUser() {
		UUID userId = UUID.randomUUID();
		Instant timestamp = Instant.parse("2026-11-25T15:00:00Z");
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);
		usuario.setCreatedAt(timestamp);
		usuario.setUpdatedAt(timestamp);

		when(currentUserService.getCurrentUser()).thenReturn(usuario);

		AuthUserResponse response = authService.getCurrentUser();

		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isEqualTo(timestamp);
		assertThat(response.updatedAt()).isEqualTo(timestamp);
	}

	@Test
	void shouldPropagateAuthenticationFailedWhenCurrentUserServiceRejects() {
		when(currentUserService.getCurrentUser())
				.thenThrow(new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas."));

		assertThatThrownBy(() -> authService.getCurrentUser())
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void shouldChangePasswordEncodeWithBCryptSaveBeforeRevokingAndRevokeAllActiveTokens() {
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashAtual");
		usuario.setId(UUID.randomUUID());

		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(passwordEncoder.matches("UniFlow@2026", "$2a$10$hashAtual")).thenReturn(true);
		when(passwordEncoder.matches("NovaSenha@2027", "$2a$10$hashAtual")).thenReturn(false);
		when(passwordEncoder.encode("NovaSenha@2027")).thenReturn("$2a$10$novoHashBCrypt");
		when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
		when(refreshTokenService.revokeAllForUser(usuario.getId())).thenReturn(2);

		authService.changePassword(new ChangePasswordRequest("UniFlow@2026", "NovaSenha@2027"));

		assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$novoHashBCrypt");
		InOrder order = inOrder(passwordEncoder, usuarioRepository, refreshTokenService);
		order.verify(passwordEncoder).matches("UniFlow@2026", "$2a$10$hashAtual");
		order.verify(passwordEncoder).matches("NovaSenha@2027", "$2a$10$hashAtual");
		order.verify(passwordEncoder).encode("NovaSenha@2027");
		order.verify(usuarioRepository).saveAndFlush(usuario);
		order.verify(refreshTokenService).revokeAllForUser(usuario.getId());
	}

	@Test
	void shouldRejectWrongCurrentPasswordWithoutMutatingHashOrRevokingTokens() {
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashAtual");
		usuario.setId(UUID.randomUUID());

		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(passwordEncoder.matches("SenhaErrada@2026", "$2a$10$hashAtual")).thenReturn(false);

		assertThatThrownBy(() -> authService.changePassword(new ChangePasswordRequest("SenhaErrada@2026", "NovaSenha@2027")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Senha atual invalida.");

		assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$hashAtual");
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void shouldRejectSamePasswordAsCurrentWithoutMutatingHashOrRevokingTokens() {
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashAtual");
		usuario.setId(UUID.randomUUID());

		when(currentUserService.getCurrentUser()).thenReturn(usuario);
		when(passwordEncoder.matches("UniFlow@2026", "$2a$10$hashAtual")).thenReturn(true);

		assertThatThrownBy(() -> authService.changePassword(new ChangePasswordRequest("UniFlow@2026", "UniFlow@2026")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("A nova senha deve ser diferente da senha atual.");

		assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$hashAtual");
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).saveAndFlush(any());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void shouldPropagateAuthenticationFailedWhenChangingPasswordWithoutAuthenticatedUser() {
		when(currentUserService.getCurrentUser())
				.thenThrow(new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas."));

		assertThatThrownBy(() -> authService.changePassword(new ChangePasswordRequest("UniFlow@2026", "NovaSenha@2027")))
				.isInstanceOf(AuthenticationFailedException.class);

		verifyNoInteractions(passwordEncoder, usuarioRepository, refreshTokenService);
	}

	@Test
	void shouldRejectNullRequestsFailFastWithoutSideEffects() {
		assertThatThrownBy(() -> authService.register(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> authService.login(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> authService.changePassword(null)).isInstanceOf(NullPointerException.class);

		verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService, refreshTokenService, currentUserService);
	}
}
