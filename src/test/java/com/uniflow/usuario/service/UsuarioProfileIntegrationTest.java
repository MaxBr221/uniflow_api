package com.uniflow.usuario.service;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UsuarioProfileIntegrationTest extends AbstractIntegrationTest {

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
	void shouldConsultAndUpdateAuthenticatedUserProfileEnforcingIsolationAndAllowedFields() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		AuthTokenResponse userA = registerAndLogin("Ana Carolina", "ana.perfil@uniflow.edu");
		AuthTokenResponse userB = registerAndLogin("Bruno Martins", "bruno.perfil@uniflow.edu");
		UUID userAId = userA.usuario().id();
		UUID userBId = userB.usuario().id();
		String originalHashA = usuarioRepository.findById(userAId).orElseThrow().getSenhaHash();

		// 1. Consulta GET /api/v1/usuarios/me ignora ?usuarioId=<B> e retorna somente o perfil de A
		mockMvc.perform(get("/api/v1/usuarios/me")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Carolina"))
				.andExpect(jsonPath("$.email").value("ana.perfil@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		// 2. PATCH /api/v1/usuarios/me atualiza nome e ignora tentativa de alterar id/status/senha/senhaHash/usuarioId
		mockMvc.perform(patch("/api/v1/usuarios/me")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "id": "%s",
								  "usuarioId": "%s",
								  "nome": "  Ana Carolina Souza  ",
								  "status": "BLOQUEADO",
								  "senha": "SenhaForjada@999",
								  "senhaHash": "$2a$10$hashForjado"
								}
								""".formatted(userBId, userBId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Carolina Souza"))
				.andExpect(jsonPath("$.email").value("ana.perfil@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"));

		Usuario userADb = usuarioRepository.findById(userAId).orElseThrow();
		Usuario userBDb = usuarioRepository.findById(userBId).orElseThrow();
		assertThat(userADb.getNome()).isEqualTo("Ana Carolina Souza");
		assertThat(userADb.getStatus()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(userADb.getSenhaHash()).isEqualTo(originalHashA).isNotEqualTo("$2a$10$hashForjado");
		assertThat(userADb.getUpdatedAt()).isAfter(userADb.getCreatedAt());
		assertThat(userBDb.getNome()).isEqualTo("Bruno Martins");
		assertThat(userBDb.getEmail()).isEqualTo("bruno.perfil@uniflow.edu");

		// 3. Atualizacao de e-mail para endereco ja pertencente a B retorna 409 CONFLICT sem corromper A nem B
		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"BRUNO.PERFIL@UNIFLOW.EDU\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

		assertThat(usuarioRepository.findById(userAId).orElseThrow().getEmail()).isEqualTo("ana.perfil@uniflow.edu");
		assertThat(usuarioRepository.findById(userBId).orElseThrow().getEmail()).isEqualTo("bruno.perfil@uniflow.edu");

		// 4. Atualizacao valida de e-mail persiste em lowercase, reflete em GET /usuarios/me, aceita reenvio do mesmo e-mail e invalida login pelo e-mail antigo
		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"  Ana.Souza@UniFlow.EDU  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana.souza@uniflow.edu"));

		mockMvc.perform(get("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Carolina Souza"))
				.andExpect(jsonPath("$.email").value("ana.souza@uniflow.edu"));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"  ANA.SOUZA@UNIFLOW.EDU  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana.souza@uniflow.edu"));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "ana.perfil@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "ana.souza@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.id").value(userAId.toString()))
				.andExpect(jsonPath("$.usuario.email").value("ana.souza@uniflow.edu"));
	}

	@Test
	void shouldValidateRequestBodyAndRejectBlockedAccountOnUsuariosMe() throws Exception {
		AuthTokenResponse user = registerAndLogin("Diego Nunes", "diego.perfil@uniflow.edu");

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("informe ao menos um campo permitido para atualizacao (nome ou email)"));

		Usuario entity = usuarioRepository.findById(user.usuario().id()).orElseThrow();
		entity.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(entity);

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Diego Alterado\"}"))
				.andExpect(status().isUnauthorized())
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
