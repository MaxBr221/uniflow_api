package com.uniflow.auth.security;

import com.uniflow.auth.service.JwtService;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.config.SecurityConfig;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JwtAuthenticationFilterWebMvcTest.DummyProtectedController.class)
@Import({
		JwtAuthenticationFilterWebMvcTest.DummyProtectedController.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class JwtAuthenticationFilterWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void shouldAuthenticateRequestOnceAndInjectPrincipalWhenJwtIsValidAndUserIsActive() throws Exception {
		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);

		when(jwtService.extractUserId("jwt.valido")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

		mockMvc.perform(get("/api/v1/test-jwt-protected")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.email").value("ana@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"));

		verify(usuarioRepository, times(1)).findById(userId);
	}

	@Test
	void shouldReturn401ProblemDetailWhenJwtFailsValidationOrUserIsBlocked() throws Exception {
		when(jwtService.extractUserId("jwt.invalido")).thenThrow(new JwtException("assinatura invalida"));

		mockMvc.perform(get("/api/v1/test-jwt-protected")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		UUID blockedId = UUID.randomUUID();
		Usuario blockedUser = new Usuario("Conta Bloqueada", "bloqueada@uniflow.edu", "$2a$10$hashSecreto");
		blockedUser.setId(blockedId);
		blockedUser.setStatus(StatusUsuario.BLOQUEADO);

		when(jwtService.extractUserId("jwt.bloqueado")).thenReturn(blockedId);
		when(usuarioRepository.findById(blockedId)).thenReturn(Optional.of(blockedUser));

		mockMvc.perform(get("/api/v1/test-jwt-protected")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.bloqueado"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@RestController
	@RequestMapping("/api/v1")
	static class DummyProtectedController {

		@GetMapping("/test-jwt-protected")
		Map<String, Object> protectedEndpoint(@AuthenticationPrincipal AuthenticatedUser user) {
			return Map.of(
					"id", user.id().toString(),
					"email", user.email(),
					"status", user.status().name()
			);
		}
	}
}
