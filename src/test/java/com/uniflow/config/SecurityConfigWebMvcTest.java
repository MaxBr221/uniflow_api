package com.uniflow.config;

import com.uniflow.auth.service.JwtService;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigWebMvcTest.DummySecurityController.class)
@Import({
		SecurityConfigWebMvcTest.DummySecurityController.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class SecurityConfigWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void shouldReturn401ProblemDetailAndKeepStatelessWhenUnauthenticatedOnProtectedRoute() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/private-resource"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/authentication-failed"))
				.andExpect(jsonPath("$.title").value("Nao autenticado"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Autenticacao necessaria ou credenciais invalidas."))
				.andExpect(jsonPath("$.instance").value("/api/v1/private-resource"))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andReturn();

		HttpSession session = result.getRequest().getSession(false);
		assertThat(session).isNull();
		assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
	}

	@Test
	void shouldReturn403ProblemDetailWhenAuthenticatedUserLacksPermission() throws Exception {
		mockMvc.perform(get("/api/v1/admin-only").with(user("estudante").roles("USER")))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/forbidden-operation"))
				.andExpect(jsonPath("$.title").value("Acesso negado"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.detail").value("Voce nao possui permissao para executar esta operacao."))
				.andExpect(jsonPath("$.instance").value("/api/v1/admin-only"))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")));
	}

	@Test
	void shouldAllowAuthenticatedUserOnProtectedRouteWithoutSession() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/private-resource").with(user("estudante")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	void shouldProtectAuthMeLogoutPasswordAndSwaggerDocsInTestProfile() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/v1/auth/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\":\"UniFlow@2026\",\"novaSenha\":\"UniFlow@2027\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldApplyCorsPolicyForConfiguredAllowedOriginsAndRejectUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/auth/register")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "Content-Type,Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mockMvc.perform(options("/api/v1/auth/register")
						.header("Origin", "https://malicioso.exemplo.com")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	@RestController
	@RequestMapping("/api/v1")
	static class DummySecurityController {

		@GetMapping("/private-resource")
		Map<String, Object> privateResource() {
			return Map.of("ok", true);
		}

		@GetMapping("/admin-only")
		@PreAuthorize("hasRole('ADMIN')")
		Map<String, Object> adminOnly() {
			return Map.of("admin", true);
		}
	}
}
