package com.uniflow.auth.controller;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.LogoutRequest;
import com.uniflow.auth.dto.RefreshTokenRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.security.AuthenticatedUser;
import com.uniflow.auth.service.AuthService;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void shouldRegisterStudentAndReturn201CreatedWithoutPasswordFields() throws Exception {
		UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		Instant timestamp = Instant.parse("2026-11-25T11:50:00Z");

		when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthUserResponse(
				id,
				"Ana Silva",
				"ana.silva@uniflow.edu",
				StatusUsuario.ATIVO,
				timestamp,
				timestamp
		));

		String payload = """
				{
				  "nome": "  Ana Silva  ",
				  "email": "  ANA.SILVA@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-25T11:50:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-11-25T11:50:00Z"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
		verify(authService).register(captor.capture());
		assertThat(captor.getValue().nome()).isEqualTo("Ana Silva");
		assertThat(captor.getValue().email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(captor.getValue().senha()).isEqualTo("UniFlow@2026");
	}

	@Test
	void shouldReturn409ConflictProblemDetailWhenEmailAlreadyExists() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenThrow(new ConflictException("Ja existe um usuario cadastrado com este e-mail."));

		String payload = """
				{
				  "nome": "Ana Silva",
				  "email": "ana.silva@uniflow.edu",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/resource-conflict"))
				.andExpect(jsonPath("$.title").value("Conflito de recurso"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("Ja existe um usuario cadastrado com este e-mail."))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void shouldReturn400ProblemDetailWhenPayloadFailsBeanValidation() throws Exception {
		String invalidPayload = """
				{
				  "nome": "A",
				  "email": "email-invalido",
				  "senha": "fraca"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidPayload))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "nome", "senha")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message", hasItem("formato de e-mail invalido")));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldReturn400ProblemDetailWhenRequestBodyIsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"nome\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldAuthenticateStudentAndReturn200WithBearerTokenAndPublicUser() throws Exception {
		UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		Instant timestamp = Instant.parse("2026-11-25T12:00:00Z");

		when(authService.login(any(LoginRequest.class))).thenReturn(AuthTokenResponse.bearer(
				"eyJhbGciOiJIUzI1NiJ9.payload.assinatura",
				"rft_exemplo_token",
				900L,
				new AuthUserResponse(id, "Ana Silva", "ana.silva@uniflow.edu", StatusUsuario.ATIVO, timestamp, timestamp)
		));

		String payload = """
				{
				  "email": "  ANA.SILVA@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9.payload.assinatura"))
				.andExpect(jsonPath("$.refreshToken").value("rft_exemplo_token"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.usuario.id").value(id.toString()))
				.andExpect(jsonPath("$.usuario.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.usuario.status").value("ATIVO"))
				.andExpect(jsonPath("$.usuario.senha").doesNotExist())
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());

		ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
		verify(authService).login(captor.capture());
		assertThat(captor.getValue().email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(captor.getValue().senha()).isEqualTo("UniFlow@2026");
	}

	@Test
	void shouldReturn401ProblemDetailWhenCredentialsAreInvalid() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new AuthenticationFailedException("Credenciais invalidas."));

		String payload = """
				{
				  "email": "ana.silva@uniflow.edu",
				  "senha": "SenhaErrada@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/authentication-failed"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Credenciais invalidas."))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")));
	}

	@Test
	void shouldReturn400ProblemDetailWhenLoginPayloadIsInvalid() throws Exception {
		String invalidPayload = """
				{
				  "email": "invalido",
				  "senha": ""
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidPayload))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "senha")));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldRenewSessionAndReturn200WithRotatedTokensAndPublicUser() throws Exception {
		UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		Instant timestamp = Instant.parse("2026-11-25T13:00:00Z");

		when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(AuthTokenResponse.bearer(
				"eyJhbGciOiJIUzI1NiJ9.novo.access",
				"rft_novo_rotacionado",
				900L,
				new AuthUserResponse(id, "Ana Silva", "ana.silva@uniflow.edu", StatusUsuario.ATIVO, timestamp, timestamp)
		));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"  rft_atual_valido  \"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9.novo.access"))
				.andExpect(jsonPath("$.refreshToken").value("rft_novo_rotacionado"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.usuario.id").value(id.toString()))
				.andExpect(jsonPath("$.usuario.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.usuario.senha").doesNotExist())
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());

		ArgumentCaptor<RefreshTokenRequest> captor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
		verify(authService).refresh(captor.capture());
		assertThat(captor.getValue().refreshToken()).isEqualTo("rft_atual_valido");
	}

	@Test
	void shouldReturn400ProblemDetailWhenRefreshTokenPayloadIsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/refresh"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("refreshToken")));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldReturn401ProblemDetailWhenRefreshTokenIsRejectedOrReused() throws Exception {
		when(authService.refresh(any(RefreshTokenRequest.class)))
				.thenThrow(new AuthenticationFailedException("Refresh token invalido ou expirado."));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"rft_reutilizado\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/authentication-failed"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Refresh token invalido ou expirado."))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/refresh"))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldLogoutSpecificSessionAndClearSecurityContextWhenAuthenticated() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		setAuthenticatedUser(userId, "ana.silva@uniflow.edu");

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"  rft_sessao_atual  \"}"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		ArgumentCaptor<LogoutRequest> captor = ArgumentCaptor.forClass(LogoutRequest.class);
		verify(authService).logout(eq(userId), captor.capture());
		assertThat(captor.getValue().refreshToken()).isEqualTo("rft_sessao_atual");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void shouldLogoutAllActiveSessionsWhenRequestBodyIsOmitted() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		setAuthenticatedUser(userId, "ana.silva@uniflow.edu");

		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(authService).logout(eq(userId), isNull());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void shouldReturn400ProblemDetailWhenLogoutPayloadHasBlankRefreshToken() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		setAuthenticatedUser(userId, "ana.silva@uniflow.edu");

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/logout"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("refreshToken")));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldReturn403ProblemDetailWhenLogoutTargetsAnotherUsersRefreshToken() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		setAuthenticatedUser(userId, "ana.silva@uniflow.edu");

		doThrow(new UnauthorizedOperationException("Voce nao possui permissao para revogar este refresh token."))
				.when(authService).logout(eq(userId), any(LogoutRequest.class));

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"rft_outro_usuario\"}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/forbidden-operation"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/logout"));
	}

	@Test
	void shouldReturnCurrentAuthenticatedUserOnGetAuthMeWithoutPasswordFields() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		Instant timestamp = Instant.parse("2026-11-25T15:00:00Z");

		when(authService.getCurrentUser()).thenReturn(new AuthUserResponse(
				userId,
				"Ana Silva",
				"ana.silva@uniflow.edu",
				StatusUsuario.ATIVO,
				timestamp,
				timestamp
		));

		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-25T15:00:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-11-25T15:00:00Z"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		verify(authService).getCurrentUser();
	}

	@Test
	void shouldReturn401ProblemDetailWhenGetAuthMeFailsAuthenticationInService() throws Exception {
		when(authService.getCurrentUser())
				.thenThrow(new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas."));

		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/me"));
	}

	private static void setAuthenticatedUser(UUID userId, String email) {
		AuthenticatedUser principal = new AuthenticatedUser(userId, email, StatusUsuario.ATIVO);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
		);
		SecurityContextHolder.setContext(context);
	}
}
