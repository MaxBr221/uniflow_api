package com.uniflow.auth.dto;

import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.usuario.enums.StatusUsuario;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginDtoWebMvcTest.DummyLoginController.class)
@Import({LoginDtoWebMvcTest.DummyLoginController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LoginDtoWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldDeserializeNormalizeValidateLoginRequestAndSerializeAuthTokenResponse() throws Exception {
		String validJson = """
				{
				  "email": "  ANA.SILVA@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/test-auth-dto/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validJson))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("access-token-exemplo"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token-exemplo"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.usuario.id").value("550e8400-e29b-41d4-a716-446655440000"))
				.andExpect(jsonPath("$.usuario.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.usuario.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.usuario.status").value("ATIVO"))
				.andExpect(jsonPath("$.usuario.createdAt").value("2026-11-23T10:00:00Z"))
				.andExpect(jsonPath("$.usuario.updatedAt").value("2026-11-23T10:00:00Z"))
				.andExpect(jsonPath("$.usuario.senha").doesNotExist())
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
	}

	@Test
	void shouldReturnProblemDetail400WithFieldErrorsWhenLoginRequestIsInvalid() throws Exception {
		String invalidJson = """
				{
				  "email": "invalido",
				  "senha": "   "
				}
				""";

		mockMvc.perform(post("/test-auth-dto/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "senha")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message", hasItem("formato de e-mail invalido")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'senha')].message", hasItem("senha e obrigatoria")));
	}

	@RestController
	@RequestMapping("/test-auth-dto")
	static class DummyLoginController {

		@PostMapping("/login")
		AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
			Instant fixedTimestamp = Instant.parse("2026-11-23T10:00:00Z");
			AuthUserResponse user = new AuthUserResponse(
					UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
					"Ana Silva",
					request.email(),
					StatusUsuario.ATIVO,
					fixedTimestamp,
					fixedTimestamp
			);
			return AuthTokenResponse.bearer(
					"access-token-exemplo",
					"refresh-token-exemplo",
					900L,
					user
			);
		}
	}
}
