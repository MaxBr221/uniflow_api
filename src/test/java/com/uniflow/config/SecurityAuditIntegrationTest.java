package com.uniflow.config;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria automatizada de seguranca e isolamento de ownership sobre toda a API ({@code BE-030}).
 */
@AutoConfigureMockMvc
class SecurityAuditIntegrationTest extends AbstractIntegrationTest {

	private static final Set<String> PUBLIC_POST_ROUTES = Set.of(SecurityConfig.PUBLIC_AUTH_POST_ENDPOINTS);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Autowired
	private ApplicationContext applicationContext;

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
	void allPrivateEndpointsMustRejectUnauthenticatedCallsAndControllersMustNotExposeOrBindJpaEntities() throws Exception {
		Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
		assertThat(controllers).isNotEmpty();

		for (Object controllerBean : controllers.values()) {
			Class<?> controllerType = controllerBean.getClass();
			if (!controllerType.getPackageName().startsWith("com.uniflow")) {
				continue;
			}
			for (Field field : controllerType.getDeclaredFields()) {
				assertThat(Repository.class.isAssignableFrom(field.getType()))
						.as("Controller %s nao deve injetar repositorio diretamente (%s)", controllerType.getSimpleName(), field.getName())
						.isFalse();
			}
		}

		List<String> auditedPrivateRoutes = new ArrayList<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
			RequestMappingInfo mapping = entry.getKey();
			HandlerMethod handlerMethod = entry.getValue();
			if (!handlerMethod.getBeanType().getPackageName().startsWith("com.uniflow")) {
				continue;
			}

			for (Parameter parameter : handlerMethod.getMethod().getParameters()) {
				assertThat(parameter.getType().isAnnotationPresent(Entity.class))
						.as("Endpoint %s nao pode receber entidade JPA diretamente (%s)", handlerMethod.getMethod().getName(), parameter.getType().getName())
						.isFalse();
			}
			assertTypeDoesNotContainJpaEntity(
					ResolvableType.forMethodReturnType(handlerMethod.getMethod()),
					handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName()
			);

			Set<RequestMethod> httpMethods = mapping.getMethodsCondition().getMethods();
			for (String pattern : mapping.getPatternValues()) {
				if (!pattern.startsWith("/api/v1/")) {
					continue;
				}
				String concretePath = pattern.replaceAll("\\{[^}]+\\}", UUID.randomUUID().toString());
				for (RequestMethod requestMethod : httpMethods) {
					boolean isPublic = requestMethod == RequestMethod.POST && PUBLIC_POST_ROUTES.contains(pattern);
					if (isPublic) {
						continue;
					}
					assertThat(handlerMethod.getMethodAnnotation(SecurityRequirements.class))
							.as("Rota privada %s %s nao pode limpar @SecurityRequirements", requestMethod, pattern)
							.isNull();

					auditedPrivateRoutes.add(requestMethod.name() + " " + pattern);
					mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(requestMethod.name()), concretePath))
							.andExpect(status().isUnauthorized())
							.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
							.andExpect(jsonPath("$.status").value(401))
							.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
				}
			}
		}

		assertThat(auditedPrivateRoutes).containsExactlyInAnyOrder(
				"GET /api/v1/auth/me",
				"POST /api/v1/auth/logout",
				"PATCH /api/v1/auth/password",
				"GET /api/v1/usuarios/me",
				"PATCH /api/v1/usuarios/me"
		);
	}

	@Test
	void allPrivateRoutesMustIgnoreClientSuppliedUsuarioIdAndPreventCrossUserStateModification() throws Exception {
		AuthTokenResponse userA = registerAndLogin("Ana Auditoria", "ana.auditoria@uniflow.edu");
		AuthTokenResponse userB = registerAndLogin("Bruno Auditoria", "bruno.auditoria@uniflow.edu");
		UUID userAId = userA.usuario().id();
		UUID userBId = userB.usuario().id();

		Usuario userBOriginal = usuarioRepository.findById(userBId).orElseThrow();
		String originalNomeB = userBOriginal.getNome();
		String originalEmailB = userBOriginal.getEmail();
		String originalHashB = userBOriginal.getSenhaHash();
		StatusUsuario originalStatusB = userBOriginal.getStatus();

		mockMvc.perform(get("/api/v1/auth/me")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.email").value("ana.auditoria@uniflow.edu"));

		mockMvc.perform(get("/api/v1/usuarios/me")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.email").value("ana.auditoria@uniflow.edu"));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "id": "%s",
								  "usuarioId": "%s",
								  "nome": "Ana Atualizada Isolada"
								}
								""".formatted(userBId, userBId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userAId.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Atualizada Isolada"));

		mockMvc.perform(patch("/api/v1/auth/password")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "id": "%s",
								  "usuarioId": "%s",
								  "senhaAtual": "UniFlow@2026",
								  "novaSenha": "NovaSenha@2027"
								}
								""".formatted(userBId, userBId)))
				.andExpect(status().isNoContent());

		// Tentativa de logout direcionado ao refresh token de Bruno deve retornar 403 FORBIDDEN_OPERATION
		mockMvc.perform(post("/api/v1/auth/logout")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + userB.refreshToken() + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));

		// Logout sem token especifico e ?usuarioId=<B> afeta somente sessoes restantes de A, preservando B intacto
		mockMvc.perform(post("/api/v1/auth/logout")
						.queryParam("usuarioId", userBId.toString())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userA.accessToken()))
				.andExpect(status().isNoContent());

		Usuario userBAfter = usuarioRepository.findById(userBId).orElseThrow();
		assertThat(userBAfter.getNome()).isEqualTo(originalNomeB);
		assertThat(userBAfter.getEmail()).isEqualTo(originalEmailB);
		assertThat(userBAfter.getSenhaHash()).isEqualTo(originalHashB);
		assertThat(userBAfter.getStatus()).isEqualTo(originalStatusB);
		assertThat(passwordEncoder.matches("UniFlow@2026", userBAfter.getSenhaHash())).isTrue();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userBId)).isEqualTo(1);
		assertThat(refreshTokenRepository.findAllByUsuarioIdOrderByCreatedAtDesc(userBId))
				.hasSize(1)
				.allSatisfy(rt -> assertThat(rt.isRevoked()).isFalse());

		mockMvc.perform(get("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + userB.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userBId.toString()))
				.andExpect(jsonPath("$.nome").value(originalNomeB));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + userB.refreshToken() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.id").value(userBId.toString()));
	}

	private static void assertTypeDoesNotContainJpaEntity(ResolvableType type, String location) {
		Class<?> resolved = type.resolve();
		if (resolved != null) {
			assertThat(resolved.isAnnotationPresent(Entity.class))
					.as("Retorno de %s nao pode expor entidade JPA (%s)", location, resolved.getName())
					.isFalse();
		}
		for (ResolvableType generic : type.getGenerics()) {
			assertTypeDoesNotContainJpaEntity(generic, location);
		}
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
