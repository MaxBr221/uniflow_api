package com.uniflow.usuario.controller;

import com.uniflow.auth.service.JwtService;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.config.SecurityConfig;
import com.uniflow.usuario.dto.UpdateProfileRequest;
import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import com.uniflow.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@Import({
		UsuarioController.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class UsuarioControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioService usuarioService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void shouldRejectUnauthenticatedGetAndPatchWith401ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Ana Maria\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		verifyNoInteractions(usuarioService);
	}

	@Test
	void shouldReturnProfileOnGetUsuariosMeWhenAuthenticated() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		stubValidJwtAuthentication(userId, "ana.silva@uniflow.edu");

		Instant timestamp = Instant.parse("2026-11-25T15:50:00Z");
		when(usuarioService.getProfile()).thenReturn(new UsuarioResponse(
				userId,
				"Ana Silva",
				"ana.silva@uniflow.edu",
				StatusUsuario.ATIVO,
				timestamp,
				timestamp
		));

		mockMvc.perform(get("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-25T15:50:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-11-25T15:50:00Z"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		verify(usuarioService).getProfile();
	}

	@Test
	void shouldUpdateProfileAndReturn200WithNormalizedFields() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		stubValidJwtAuthentication(userId, "ana.silva@uniflow.edu");

		Instant created = Instant.parse("2026-11-22T10:00:00Z");
		Instant updated = Instant.parse("2026-11-25T15:55:00Z");
		when(usuarioService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(new UsuarioResponse(
				userId,
				"Ana Maria Silva",
				"ana.maria@uniflow.edu",
				StatusUsuario.ATIVO,
				created,
				updated
		));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "  Ana Maria Silva  ",
								  "email": "  ANA.MARIA@UniFlow.EDU  "
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.nome").value("Ana Maria Silva"))
				.andExpect(jsonPath("$.email").value("ana.maria@uniflow.edu"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());

		ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
		verify(usuarioService).updateProfile(captor.capture());
		assertThat(captor.getValue().nome()).isEqualTo("Ana Maria Silva");
		assertThat(captor.getValue().email()).isEqualTo("ana.maria@uniflow.edu");
	}

	@Test
	void shouldReturn400ForInvalidPayloadAnd409WhenEmailConflicts() throws Exception {
		UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		stubValidJwtAuthentication(userId, "ana.silva@uniflow.edu");

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.instance").value("/api/v1/usuarios/me"));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"A\",\"email\":\"email-invalido\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("nome")))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));

		verifyNoInteractions(usuarioService);

		when(usuarioService.updateProfile(any(UpdateProfileRequest.class)))
				.thenThrow(new ConflictException("Ja existe um usuario cadastrado com este e-mail."));

		mockMvc.perform(patch("/api/v1/usuarios/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"duplicado@uniflow.edu\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
	}

	private void stubValidJwtAuthentication(UUID userId, String email) {
		Usuario usuario = new Usuario("Ana Silva", email, "$2a$10$hashSecreto");
		usuario.setId(userId);
		when(jwtService.extractUserId("jwt.valido")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
	}
}
