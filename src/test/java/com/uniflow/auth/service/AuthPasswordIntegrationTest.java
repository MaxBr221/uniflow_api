package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthPasswordIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldChangePasswordWithBCryptRevokeAllOwnRefreshTokensAndBlockOldPassword() throws Exception {
		mockMvc.perform(patch("/api/v1/auth/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		registerUser("Ana Senha", "ana.senha@uniflow.edu");
		registerUser("Bruno Senha", "bruno.senha@uniflow.edu");

		AuthTokenResponse sessionA1 = loginUser("ana.senha@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse sessionA2 = loginUser("ana.senha@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse sessionB = loginUser("bruno.senha@uniflow.edu", "UniFlow@2026");

		UUID userAId = sessionA1.usuario().id();
		UUID userBId = sessionB.usuario().id();

		Usuario userABefore = usuarioRepository.findById(userAId).orElseThrow();
		Usuario userBBefore = usuarioRepository.findById(userBId).orElseThrow();
		String hashABefore = userABefore.getSenhaHash();
		Instant updatedAtABefore = userABefore.getUpdatedAt();
		String hashBBefore = userBBefore.getSenhaHash();

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);

		// Mesmo enviando ?usuarioId=<B> e campos forjados no JSON, altera somente o usuario autenticado no SecurityContext
		mockMvc.perform(patch("/api/v1/auth/password")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionA1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "id": "%s",
								  "usuarioId": "%s",
								  "senhaAtual": "UniFlow@2026",
								  "novaSenha": "NovaSenha@2027"
								}
								""".formatted(userBId, userBId)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		Usuario userAAfter = usuarioRepository.findById(userAId).orElseThrow();
		Usuario userBAfter = usuarioRepository.findById(userBId).orElseThrow();

		assertThat(userAAfter.getSenhaHash()).startsWith("$2a$10$");
		assertThat(userAAfter.getSenhaHash()).isNotEqualTo(hashABefore);
		assertThat(userAAfter.getSenhaHash()).isNotEqualTo("NovaSenha@2027");
		assertThat(passwordEncoder.matches("NovaSenha@2027", userAAfter.getSenhaHash())).isTrue();
		assertThat(passwordEncoder.matches("UniFlow@2026", userAAfter.getSenhaHash())).isFalse();
		assertThat(userAAfter.getUpdatedAt()).isAfter(updatedAtABefore);

		assertThat(userBAfter.getSenhaHash()).isEqualTo(hashBBefore);

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isZero();
		List<RefreshToken> userATokens = refreshTokenRepository.findAllByUsuarioIdOrderByCreatedAtDesc(userAId);
		assertThat(userATokens).hasSize(2);
		assertThat(userATokens).allSatisfy(token -> {
			assertThat(token.isRevoked()).isTrue();
			assertThat(token.getRevokedAt()).isNotNull();
		});
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);

		// Ambos refresh tokens antigos de A sao rejeitados com 401 apos a troca de senha
		for (String revokedRefresh : List.of(sessionA1.refreshToken(), sessionA2.refreshToken())) {
			mockMvc.perform(post("/api/v1/auth/refresh")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"refreshToken\":\"" + revokedRefresh + "\"}"))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		}

		// Refresh token de B continua valido e rotaciona normalmente
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + sessionB.refreshToken() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.id").value(userBId.toString()));

		// Senha antiga de A e rejeitada no login; nova senha autentica e libera rotas protegidas
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ana.senha@uniflow.edu\",\"senha\":\"UniFlow@2026\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		AuthTokenResponse newLogin = loginUser("ana.senha@uniflow.edu", "NovaSenha@2027");
		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + newLogin.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.email").value("ana.senha@uniflow.edu"));
	}

	@Test
	void shouldRejectInvalidPayloadsWrongCurrentPasswordSamePasswordAndBlockedUserWithoutMutatingState() throws Exception {
		registerUser("Carlos Senha", "carlos.senha@uniflow.edu");
		AuthTokenResponse session = loginUser("carlos.senha@uniflow.edu", "UniFlow@2026");
		UUID userId = session.usuario().id();
		String originalHash = usuarioRepository.findById(userId).orElseThrow().getSenhaHash();

		// 1. Payload vazio {} -> 400 VALIDATION_ERROR detalhando os dois campos
		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("senhaAtual", "novaSenha")));

		// 2. Nova senha fraca -> 400 VALIDATION_ERROR
		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"fraca\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("novaSenha"));

		// 3. Senha atual incorreta -> 422 BUSINESS_RULE_VIOLATION, sem vazar segredos e sem revogar sessao
		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"SenhaErrada@2026\",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
				.andExpect(content().string(not(containsString("SenhaErrada@2026"))))
				.andExpect(content().string(not(containsString("NovaSenha@2027"))))
				.andExpect(content().string(not(containsString("$2a$"))));

		// 4. Nova senha igual a senha atual -> 422 BUSINESS_RULE_VIOLATION
		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"UniFlow@2026\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

		Usuario unchanged = usuarioRepository.findById(userId).orElseThrow();
		assertThat(unchanged.getSenhaHash()).isEqualTo(originalHash);
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(1);
		loginUser("carlos.senha@uniflow.edu", "UniFlow@2026");

		// 5. Conta bloqueada recebe 401 mesmo com access token assinado
		unchanged.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(unchanged);

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"NovaSenha@2027\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		assertThat(usuarioRepository.findById(userId).orElseThrow().getSenhaHash()).isEqualTo(originalHash);
	}

	private void registerUser(String nome, String email) throws Exception {
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
	}

	private AuthTokenResponse loginUser(String email, String senha) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "senha": "%s"
								}
								""".formatted(email, senha)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthTokenResponse.class);
	}
}
