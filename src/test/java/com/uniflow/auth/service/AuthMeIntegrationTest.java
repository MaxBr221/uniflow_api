package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthMeIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldReturnCurrentAuthenticatedUserFromSecurityContextAndIgnoreClientUsuarioId() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		AuthTokenResponse sessionA = registerAndLogin("Camila Duarte", "camila.me@uniflow.edu");
		AuthTokenResponse sessionB = registerAndLogin("BrunoCosta", "bruno.me@uniflow.edu");

		// Mesmo passando ?usuarioId=<B> na query, o backend le exclusivamente o SecurityContext do JWT de A
		mockMvc.perform(get("/api/v1/auth/me")
						.queryParam("usuarioId", sessionB.usuario().id().toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionA.accessToken()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(sessionA.usuario().id().toString()))
				.andExpect(jsonPath("$.nome").value("Camila Duarte"))
				.andExpect(jsonPath("$.email").value("camila.me@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.createdAt", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$")))
				.andExpect(jsonPath("$.updatedAt", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$")))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionB.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(sessionB.usuario().id().toString()))
				.andExpect(jsonPath("$.email").value("bruno.me@uniflow.edu"));
	}

	@Test
	void shouldReturn401ProblemDetailWhenJwtIsInvalidOrAccountIsBlocked() throws Exception {
		AuthTokenResponse session = registerAndLogin("Sofia Reis", "sofia.bloqueio@uniflow.edu");

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken() + "adulterado"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		Usuario usuario = usuarioRepository.findById(session.usuario().id()).orElseThrow();
		usuario.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(usuario);

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	private AuthTokenResponse registerAndLogin(String nome, String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "%s",
								  "email": "%s",
								  "senha": "UniFlow@2026"
								}
								""".formatted(nome, email)))
				.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "senha": "UniFlow@2026"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthTokenResponse.class);
	}
}
