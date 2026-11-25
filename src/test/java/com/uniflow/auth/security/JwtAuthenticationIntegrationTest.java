package com.uniflow.auth.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest extends AbstractIntegrationTest {

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
	void shouldAuthenticateProtectedRouteWithRealJwtAndRejectTamperedOrBlockedAccountTokens() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Gabriel Alves",
								  "email": "gabriel.alves@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "gabriel.alves@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		AuthTokenResponse tokenResponse = objectMapper.readValue(
				loginResult.getResponse().getContentAsString(),
				AuthTokenResponse.class
		);
		String validJwt = tokenResponse.accessToken();

		// 1. Token valido + usuario ATIVO passa da camada Spring Security (chega ao DispatcherServlet -> 404 ROUTE_NOT_FOUND enquanto /semestres nao existe)
		mockMvc.perform(get("/api/v1/semestres")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));

		// 2. Token adulterado falha assinatura e recebe 401 ProblemDetail
		mockMvc.perform(get("/api/v1/semestres")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt + "adulterado"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		// 3. Mesmo token valido passa a receber 401 apos usuario ser bloqueado no banco
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase("gabriel.alves@uniflow.edu").orElseThrow();
		usuario.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(usuario);

		mockMvc.perform(get("/api/v1/semestres")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		// 4. Rota publica continua acessivel (200 OK) mesmo se cliente carregar Bearer expirado/invalido no header
		usuario.setStatus(StatusUsuario.ATIVO);
		usuarioRepository.saveAndFlush(usuario);

		mockMvc.perform(post("/api/v1/auth/login")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt + "invalido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "gabriel.alves@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}
}
