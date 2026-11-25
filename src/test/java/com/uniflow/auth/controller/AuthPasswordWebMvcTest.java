package com.uniflow.auth.controller;

import com.uniflow.auth.dto.ChangePasswordRequest;
import com.uniflow.auth.service.AuthService;
import com.uniflow.auth.service.JwtService;
import com.uniflow.common.exception.BusinessRuleException;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.config.SecurityConfig;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({
		AuthController.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class AuthPasswordWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void shouldRejectUnauthenticatedPatchPasswordWith401ProblemDetail() throws Exception {
		mockMvc.perform(patch("/api/v1/auth/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/password"));

		verifyNoInteractions(authService);
	}

	@Test
	void shouldChangePasswordAndReturn204NoContentWhenAuthenticatedAndValid() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		stubValidJwtAuthentication(userId, "ana.silva@uniflow.edu");
		doNothing().when(authService).changePassword(any(ChangePasswordRequest.class));

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\" UniFlow@2026 \",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		ArgumentCaptor<ChangePasswordRequest> captor = ArgumentCaptor.forClass(ChangePasswordRequest.class);
		verify(authService).changePassword(captor.capture());
		assertThat(captor.getValue().senhaAtual()).isEqualTo(" UniFlow@2026 ");
		assertThat(captor.getValue().novaSenha()).isEqualTo("NovaSenha@2027");
	}

	@Test
	void shouldReturn400ForInvalidPayloadsAnd422ForBusinessRuleViolation() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		stubValidJwtAuthentication(userId, "ana.silva@uniflow.edu");

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"   \",\"novaSenha\":\"fraca\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/password"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("novaSenha", "senhaAtual")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'senhaAtual')].message", hasItem("senhaAtual e obrigatoria")));

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"senhaAtual\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));

		verifyNoInteractions(authService);

		doThrow(new BusinessRuleException("Senha atual invalida."))
				.when(authService).changePassword(any(ChangePasswordRequest.class));

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"Errada@2026\",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/business-rule-violation"))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
				.andExpect(jsonPath("$.detail").value("Senha atual invalida."))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/password"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(content().string(not(containsString("Errada@2026"))))
				.andExpect(content().string(not(containsString("NovaSenha@2027"))));
	}

	private void stubValidJwtAuthentication(UUID userId, String email) {
		Usuario usuario = new Usuario("Ana Silva", email, "$2a$10$hashSecreto");
		usuario.setId(userId);
		when(jwtService.extractUserId("jwt.valido")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
	}
}
