package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite ponta a ponta consolidada do Epico 2 ({@code BE-031}) exercitando todo o ciclo de
 * cadastro, e-mail duplicado, login multi-dispositivo, rotacao em cadeia de refresh token,
 * deteccao de reuso sem rollback, logout seletivo/global, perfil, troca de senha e matriz {@code 401}/{@code 403}.
 */
@AutoConfigureMockMvc
class AuthEndToEndFlowIntegrationTest extends AbstractIntegrationTest {

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

	@Value("${app.security.jwt.secret}")
	private String jwtSecret;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldExecuteCompleteMultiDeviceAuthLifecycleRotationReuseBlockLogoutProfileAndPasswordChange() throws Exception {
		// 1. Cadastro inicial de dois estudantes independentes + bloqueio de duplicidade (409)
		AuthUserResponse registeredA = registerStudent("  Laura Martins  ", "  Laura.Martins@UniFlow.EDU  ", "UniFlow@2026");
		AuthUserResponse registeredB = registerStudent("Pedro Almeida", "pedro.almeida@uniflow.edu", "UniFlow@2026");
		UUID userAId = registeredA.id();
		UUID userBId = registeredB.id();

		assertThat(registeredA.email()).isEqualTo("laura.martins@uniflow.edu");
		assertThat(refreshTokenRepository.count()).isZero();

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Tentativa Duplicada",
								  "email": "LAURA.MARTINS@uniflow.edu",
								  "senha": "UniFlow@2026"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

		// 2. Login em dois dispositivos de Laura + um dispositivo de Pedro
		AuthTokenResponse deviceA1 = loginStudent("LAURA.MARTINS@UNIFLOW.EDU", "UniFlow@2026");
		AuthTokenResponse deviceA2 = loginStudent("laura.martins@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse deviceB1 = loginStudent("pedro.almeida@uniflow.edu", "UniFlow@2026");

		assertThat(deviceA1.accessToken()).isNotEqualTo(deviceA2.accessToken());
		assertThat(deviceA1.refreshToken()).isNotEqualTo(deviceA2.refreshToken());
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);

		// 3. Cadeia multipla de rotacao no Dispositivo 1 (R1 -> R2 -> R3) preservando Dispositivo 2 ativo
		String r1 = deviceA1.refreshToken();
		AuthTokenResponse rot1 = refreshSession(r1);
		String r2 = rot1.refreshToken();
		AuthTokenResponse rot2 = refreshSession(r2);
		String r3 = rot2.refreshToken();

		RefreshToken dbR1 = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(r1)).orElseThrow();
		RefreshToken dbR2 = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(r2)).orElseThrow();
		RefreshToken dbR3 = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(r3)).orElseThrow();
		assertThat(dbR1.isRevoked()).isTrue();
		assertThat(dbR1.getReplacedByTokenId()).isEqualTo(dbR2.getId());
		assertThat(dbR2.isRevoked()).isTrue();
		assertThat(dbR2.getReplacedByTokenId()).isEqualTo(dbR3.getId());
		assertThat(dbR3.isRevoked()).isFalse();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);

		// 4. Atualizacao de e-mail em /usuarios/me mantem sessao ativa por UUID e atualiza claim email na proxima rotacao
		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + rot2.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Laura Martins Atualizada",
								  "email": "  Laura.Nova@UniFlow.EDU  "
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("laura.nova@uniflow.edu"));

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + rot2.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.nome").value("Laura Martins Atualizada"))
				.andExpect(jsonPath("$.email").value("laura.nova@uniflow.edu"));

		AuthTokenResponse rot3 = refreshSession(r3);
		String r4 = rot3.refreshToken();
		Claims claimsAfterEmailUpdate = jwtService.parseAndValidateAccessToken(rot3.accessToken());
		assertThat(claimsAfterEmailUpdate.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("laura.nova@uniflow.edu");

		// 5. Logout seletivo do Dispositivo 2 revoga apenas deviceA2.refreshToken(), mantendo r4 apto
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + deviceA2.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + deviceA2.refreshToken() + "\"}"))
				.andExpect(status().isNoContent());

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(1);
		AuthTokenResponse rot4 = refreshSession(r4);
		String r5 = rot4.refreshToken();
		AuthTokenResponse deviceA3 = loginStudent("laura.nova@uniflow.edu", "UniFlow@2026");
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);

		// 6. Reuso de token ancestral r1 (rotacionado saltos atras) retorna 401 e revoga TODAS as sessoes de Laura sem afetar Pedro
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + r1 + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isZero();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);

		for (String compromisedToken : List.of(r5, deviceA3.refreshToken())) {
			mockMvc.perform(post("/api/v1/auth/refresh")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"refreshToken\":\"" + compromisedToken + "\"}"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		}

		// Sessao de Pedro segue integra e rotaciona normalmente
		AuthTokenResponse rotatedB = refreshSession(deviceB1.refreshToken());
		assertThat(rotatedB.usuario().id()).isEqualTo(userBId);

		// 7. Re-login de Laura, alteracao de senha revogando tokens e logout global com JSON vazio "{}"
		AuthTokenResponse freshA1 = loginStudent("laura.nova@uniflow.edu", "UniFlow@2026");
		loginStudent("laura.nova@uniflow.edu", "UniFlow@2026");
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + freshA1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"NovaForte@2027\"}"))
				.andExpect(status().isNoContent());
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isZero();

		AuthTokenResponse postPwd1 = loginStudent("laura.nova@uniflow.edu", "NovaForte@2027");
		AuthTokenResponse postPwd2 = loginStudent("laura.nova@uniflow.edu", "NovaForte@2027");
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isEqualTo(2);

		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + postPwd1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNoContent());

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userAId)).isZero();
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + postPwd2.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void shouldEnforce401ForExpiredUnsignedWrongTypeOrMissingUserJwtAnd403EvenOnAlreadyRevokedForeignRefreshToken() throws Exception {
		registerStudent("Alice Seguranca", "alice.sec@uniflow.edu", "UniFlow@2026");
		registerStudent("Beto Seguranca", "beto.sec@uniflow.edu", "UniFlow@2026");

		AuthTokenResponse aliceSession1 = loginStudent("alice.sec@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse aliceSession2 = loginStudent("alice.sec@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse betoSession = loginStudent("beto.sec@uniflow.edu", "UniFlow@2026");
		UUID aliceId = aliceSession1.usuario().id();

		SecretKey signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();

		String expiredJwt = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(aliceId.toString())
				.claim(JwtService.CLAIM_EMAIL, "alice.sec@uniflow.edu")
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(now.minusSeconds(1800)))
				.expiration(Date.from(now.minusSeconds(60)))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();

		String wrongTypeJwt = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(aliceId.toString())
				.claim(JwtService.CLAIM_EMAIL, "alice.sec@uniflow.edu")
				.claim(JwtService.CLAIM_TOKEN_TYPE, "refresh")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(900)))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();

		String deletedUserJwt = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(UUID.randomUUID().toString())
				.claim(JwtService.CLAIM_EMAIL, "fantasma@uniflow.edu")
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(900)))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();

		String algNoneJwt = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
				+ "."
				+ Base64.getUrlEncoder().withoutPadding()
				.encodeToString(("{\"sub\":\"" + aliceId + "\",\"jti\":\"" + UUID.randomUUID()
						+ "\",\"type\":\"access\"}").getBytes(StandardCharsets.UTF_8))
				+ ".";

		for (String rejectedJwt : List.of(expiredJwt, wrongTypeJwt, deletedUserJwt, algNoneJwt)) {
			mockMvc.perform(get("/api/v1/auth/me")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + rejectedJwt))
					.andExpect(status().isUnauthorized())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

			mockMvc.perform(post("/api/v1/auth/logout")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + rejectedJwt))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

			mockMvc.perform(patch("/api/v1/auth/password")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + rejectedJwt)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"NovaForte@2027\"}"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

			mockMvc.perform(get("/api/v1/usuarios/me")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + rejectedJwt))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		}

		// Beto tenta revogar refresh token ATIVO de Alice -> 403 FORBIDDEN_OPERATION
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + betoSession.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + aliceSession1.refreshToken() + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/forbidden-operation"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/logout"));

		// Alice revoga legitimamente aliceSession1; se Beto tentar sondar aliceSession1 ja revogado, continua recebendo 403 (nunca 204)
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceSession1.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + aliceSession1.refreshToken() + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + betoSession.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + aliceSession1.refreshToken() + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));

		// Sessao restante de Alice (aliceSession2) permanece ativa e intacta
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(aliceId)).isEqualTo(1);
		assertThat(refreshSession(aliceSession2.refreshToken()).usuario().id()).isEqualTo(aliceId);
	}

	@ParameterizedTest
	@EnumSource(value = StatusUsuario.class, names = {"INATIVO", "BLOQUEADO", "EXCLUIDO"})
	void shouldBlockLoginAuthenticatedRoutesAndRefreshForEveryNonActiveAccountStatus(StatusUsuario nonActiveStatus) throws Exception {
		String email = "status." + nonActiveStatus.name().toLowerCase() + "@uniflow.edu";
		registerStudent("Conta Status", email, "UniFlow@2026");
		AuthTokenResponse activeSession = loginStudent(email, "UniFlow@2026");
		UUID userId = activeSession.usuario().id();

		Usuario usuario = usuarioRepository.findById(userId).orElseThrow();
		usuario.setStatus(nonActiveStatus);
		usuarioRepository.saveAndFlush(usuario);

		// 1. Login retorna mensagem generica 401 sem enumerar status da conta nem emitir novo refresh token
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "senha": "UniFlow@2026"
								}
								""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.detail").value("Credenciais invalidas."));

		// 2. Access token anteriormente assinado passa a ser rejeitado nas rotas autenticadas
		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + activeSession.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(get("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + activeSession.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		// 3. Refresh token anteriormente emitido falha com 401 e e revogado no banco
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + activeSession.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isZero();
	}

	@Test
	void shouldEnforceMediaType415Method405CorsPreflightAndStatelessNoHttpSession() throws Exception {
		registerStudent("Clara Protocolo", "clara.proto@uniflow.edu", "UniFlow@2026");
		AuthTokenResponse session = loginStudent("clara.proto@uniflow.edu", "UniFlow@2026");

		// 1. Content-Type invalido (text/plain) retorna 415 UNSUPPORTED_MEDIA_TYPE RFC 7807 (nunca 500)
		for (String postEndpoint : SecurityConfigPublicEndpoints.ALL) {
			mockMvc.perform(post(postEndpoint)
							.contentType(MediaType.TEXT_PLAIN)
							.content("texto invalido"))
					.andExpect(status().isUnsupportedMediaType())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.status").value(415))
					.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
		}

		mockMvc.perform(patch("/api/v1/auth/password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
						.contentType(MediaType.TEXT_PLAIN)
						.content("texto invalido"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

		// 2. Metodo HTTP incorreto: sem token cai em 401 (apenas POST e permitAll); autenticado retorna 405 METHOD_NOT_ALLOWED
		mockMvc.perform(get("/api/v1/auth/login"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(get("/api/v1/auth/login")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

		mockMvc.perform(put("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

		// 3. Preflight CORS OPTIONS permitido na origem configurada sem exigir Bearer token
		mockMvc.perform(options("/api/v1/auth/me")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

		// 4. Garantia stateless: nenhuma HttpSession ou cookie JSESSIONID criado apos autenticacao JWT
		MvcResult meResult = mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andReturn();
		assertThat(meResult.getRequest().getSession(false)).isNull();
	}

	private AuthUserResponse registerStudent(String nome, String email, String senha) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "%s",
								  "email": "%s",
								  "senha": "%s"
								}
								""".formatted(nome, email, senha)))
				.andExpect(status().isCreated())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andReturn();
		assertThat(result.getRequest().getSession(false)).isNull();
		return objectMapper.readValue(result.getResponse().getContentAsString(), AuthUserResponse.class);
	}

	private AuthTokenResponse loginStudent(String email, String senha) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "senha": "%s"
								}
								""".formatted(email, senha)))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andReturn();
		assertThat(result.getRequest().getSession(false)).isNull();
		return objectMapper.readValue(result.getResponse().getContentAsString(), AuthTokenResponse.class);
	}

	private AuthTokenResponse refreshSession(String rawRefreshToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + rawRefreshToken + "\"}"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andReturn();
		assertThat(result.getRequest().getSession(false)).isNull();
		return objectMapper.readValue(result.getResponse().getContentAsString(), AuthTokenResponse.class);
	}

	private static final class SecurityConfigPublicEndpoints {
		static final List<String> ALL = List.of(
				"/api/v1/auth/register",
				"/api/v1/auth/login",
				"/api/v1/auth/refresh"
		);
	}
}
