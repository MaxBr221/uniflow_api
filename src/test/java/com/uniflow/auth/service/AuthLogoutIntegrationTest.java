package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthLogoutIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldRevokeSpecificRefreshTokenOnLogoutKeepOtherSessionActiveAndBlockSubsequentRefresh() throws Exception {
		registerUser("Marina Logout", "marina.logout@uniflow.edu");
		AuthTokenResponse session1 = loginUser("marina.logout@uniflow.edu");
		AuthTokenResponse session2 = loginUser("marina.logout@uniflow.edu");
		UUID userId = session1.usuario().id();

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(2);

		// 1. Sem JWT recebe 401 ProblemDetail
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		// 2. Logout especifico revoga apenas session1.refreshToken() e retorna 204
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + session1.refreshToken() + "\"}"))
				.andExpect(status().isNoContent());

		RefreshToken row1 = refreshTokenRepository
				.findByTokenHash(RefreshTokenService.hashToken(session1.refreshToken()))
				.orElseThrow();
		RefreshToken row2 = refreshTokenRepository
				.findByTokenHash(RefreshTokenService.hashToken(session2.refreshToken()))
				.orElseThrow();

		assertThat(row1.isRevoked()).isTrue();
		assertThat(row1.getRevokedAt()).isNotNull();
		assertThat(row2.isRevoked()).isFalse();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(1);

		// 3. Idempotencia ao repetir logout no mesmo token ja revogado
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + session1.refreshToken() + "\"}"))
				.andExpect(status().isNoContent());

		// 4. Sessao 2 continua apta a renovar enquanto token1 revogado e bloqueado
		MvcResult rotatedSession2 = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + session2.refreshToken() + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		AuthTokenResponse session2Refreshed = objectMapper.readValue(
				rotatedSession2.getResponse().getContentAsString(),
				AuthTokenResponse.class
		);

		// 5. Tentativa de renovar usando refresh token revogado no logout falha com 401 e revoga demais tokens por seguranca
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + session1.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + session2Refreshed.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized());
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isZero();
	}

	@Test
	void shouldBlockCrossUserRefreshTokenRevocationWith403AndRevokeAllOwnTokensWhenBodyOmitted() throws Exception {
		registerUser("Usuario A", "usuario.a@uniflow.edu");
		registerUser("Usuario B", "usuario.b@uniflow.edu");

		AuthTokenResponse sessionA1 = loginUser("usuario.a@uniflow.edu");
		AuthTokenResponse sessionA2 = loginUser("usuario.a@uniflow.edu");
		AuthTokenResponse sessionB = loginUser("usuario.b@uniflow.edu");

		UUID userAId = sessionA1.usuario().id();
		UUID userBId = sessionB.usuario().id();

		// Usuario B tenta revogar o refresh token do Usuario A -> 403 FORBIDDEN_OPERATION sem alterar A
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionB.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + sessionA1.refreshToken() + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);

		// Usuario A realiza logout sem body (somente Bearer) -> revoga todos os tokens ativos de A, mantendo B intacto
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionA1.accessToken()))
				.andExpect(status().isNoContent());

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isZero();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + sessionA2.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
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

	private AuthTokenResponse loginUser(String email) throws Exception {
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
