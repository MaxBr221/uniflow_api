package com.uniflow.config;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldAllowPublicRegistrationWithoutTokenOrCsrfAndEnforceDuplicateConflictOverHttp() throws Exception {
		String payload = """
				{
				  "nome": "  Lucas Ferreira  ",
				  "email": "  Lucas.Ferreira@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.nome").value("Lucas Ferreira"))
				.andExpect(jsonPath("$.email").value("lucas.ferreira@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		assertThat(usuarioRepository.existsByEmailIgnoreCase("lucas.ferreira@uniflow.edu")).isTrue();

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
	}

	@Test
	void shouldPermitPublicLoginAndRefreshRoutesWhileReturning401ProblemDetailForProtectedEndpoints() throws Exception {
		// /auth/login liberada com permitAll: payload vazio cai no Bean Validation (400), nao em 401/403
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		// /auth/refresh liberada com permitAll (retorna 404 ate BE-025)
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/semestres"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.instance").value("/api/v1/semestres"));

		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}
}
