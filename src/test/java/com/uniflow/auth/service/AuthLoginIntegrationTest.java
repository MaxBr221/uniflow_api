package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AuthLoginIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldRegisterAndAuthenticateReturningValidSignedJwtAndPublicUserData() throws Exception {
		String registerPayload = """
				{
				  "nome": "  Beatriz Mendes  ",
				  "email": "  Beatriz.Mendes@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerPayload))
				.andExpect(status().isCreated());

		String loginPayload = """
				{
				  "email": "  BEATRIZ.MENDES@UNIFLOW.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.usuario.email").value("beatriz.mendes@uniflow.edu"))
				.andExpect(jsonPath("$.usuario.nome").value("Beatriz Mendes"))
				.andExpect(jsonPath("$.usuario.status").value("ATIVO"))
				.andExpect(jsonPath("$.usuario.senha").doesNotExist())
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
				.andReturn();

		AuthTokenResponse tokenResponse = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(),
				AuthTokenResponse.class
		);

		Claims claims = jwtService.parseAndValidateAccessToken(tokenResponse.accessToken());
		assertThat(claims.getSubject()).isEqualTo(tokenResponse.usuario().id().toString());
		assertThat(UUID.fromString(claims.getId())).isNotNull();
		assertThat(claims.getIssuedAt()).isNotNull();
		assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
		assertThat(claims.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("beatriz.mendes@uniflow.edu");
	}

	@Test
	void shouldReturnGeneric401ProblemDetailForUnknownEmailWrongPasswordAndBlockedUser() throws Exception {
		usuarioRepository.saveAndFlush(new Usuario(
				"Rafael Souza",
				"rafael@uniflow.edu",
				passwordEncoder.encode("UniFlow@2026")
		));

		Usuario bloqueado = new Usuario(
				"Conta Bloqueada",
				"bloqueado@uniflow.edu",
				passwordEncoder.encode("UniFlow@2026")
		);
		bloqueado.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(bloqueado);

		String[] invalidPayloads = {
				"""
				{ "email": "naoexiste@uniflow.edu", "senha": "UniFlow@2026" }
				""",
				"""
				{ "email": "rafael@uniflow.edu", "senha": "SenhaErrada@2026" }
				""",
				"""
				{ "email": "bloqueado@uniflow.edu", "senha": "UniFlow@2026" }
				"""
		};

		for (String payload : invalidPayloads) {
			mockMvc.perform(post("/api/v1/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
					.andExpect(jsonPath("$.detail").value("Credenciais invalidas."));
		}
	}
}
