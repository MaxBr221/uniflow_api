package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthRefreshIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldRotateSessionOverHttpAndRevokeAllActiveTokensOnReuseDetection() throws Exception {
		AuthTokenResponse login = registerAndLogin("helena.refresh@uniflow.edu");
		UUID userId = login.usuario().id();
		String access1 = login.accessToken();
		String refresh1 = login.refreshToken();

		// 1. Renovar sessao via POST /api/v1/auth/refresh (tambem tolerando header Bearer expirado/invalido em rota permitAll)
		MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
						.header(HttpHeaders.AUTHORIZATION, "Bearer token.antigo.expirado")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh1 + "\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.usuario.id").value(userId.toString()))
				.andExpect(jsonPath("$.usuario.email").value("helena.refresh@uniflow.edu"))
				.andExpect(jsonPath("$.usuario.senha").doesNotExist())
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
				.andReturn();

		AuthTokenResponse refreshed = objectMapper.readValue(
				refreshResult.getResponse().getContentAsString(),
				AuthTokenResponse.class
		);
		String access2 = refreshed.accessToken();
		String refresh2 = refreshed.refreshToken();

		assertThat(refresh2).startsWith(RefreshTokenService.TOKEN_PREFIX);
		assertThat(refresh2).isNotEqualTo(refresh1);
		Claims claims2 = jwtService.parseAndValidateAccessToken(access2);
		assertThat(claims2.getSubject()).isEqualTo(userId.toString());
		assertThat(access2).isNotEqualTo(access1);

		RefreshToken row1 = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(refresh1)).orElseThrow();
		RefreshToken row2 = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(refresh2)).orElseThrow();
		assertThat(row1.isRevoked()).isTrue();
		assertThat(row1.getReplacedByTokenId()).isEqualTo(row2.getId());
		assertThat(row2.isRevoked()).isFalse();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(1);

		// 2. Novo access token emitido na renovacao autentica rotas protegidas
		mockMvc.perform(get("/api/v1/semestres")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + access2))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));

		// 3. Reutilizar refresh1 (ja rotacionado) retorna 401 e revoga tambem refresh2 no banco (noRollbackFor)
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh1 + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isZero();

		// 4. Token substituto refresh2 passa a ser rejeitado com 401 apos deteccao de comprometimento
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh2 + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void shouldReturn400ForInvalidPayloadAnd401ForUnknownExpiredOrBlockedAccountRefresh() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("refreshToken"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"rft_inexistente_001\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		AuthTokenResponse login = registerAndLogin("igor.expirado@uniflow.edu");
		Usuario usuario = usuarioRepository.findById(login.usuario().id()).orElseThrow();

		String expiredRaw = "rft_expirado_integracao";
		RefreshToken expired = refreshTokenRepository.saveAndFlush(new RefreshToken(
				usuario,
				RefreshTokenService.hashToken(expiredRaw),
				Instant.now().minusSeconds(60),
				Instant.now().minusSeconds(120)
		));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + expiredRaw + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		assertThat(refreshTokenRepository.findById(expired.getId()).orElseThrow().isRevoked()).isTrue();

		usuario.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(usuario);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + login.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(usuario.getId())).isZero();
	}

	private AuthTokenResponse registerAndLogin(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Estudante Refresh",
								  "email": "%s",
								  "senha": "UniFlow@2026"
								}
								""".formatted(email)))
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
