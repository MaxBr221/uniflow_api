package com.uniflow.auth.controller;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.service.AuthService;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
				null,
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
}
