package com.uniflow.auth.dto;

import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.usuario.enums.StatusUsuario;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegisterDtoWebMvcTest.DummyRegisterController.class)
@Import({RegisterDtoWebMvcTest.DummyRegisterController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RegisterDtoWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldDeserializeNormalizeValidateAndSerializeAuthUserResponseWithoutSensitiveFields() throws Exception {
		String validJson = """
				{
				  "nome": "  Ana Silva  ",
				  "email": "  ANA.SILVA@UniFlow.EDU  ",
				  "senha": "UniFlow@2026"
				}
				""";

		mockMvc.perform(post("/test-auth-dto/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validJson))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.nome").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana.silva@uniflow.edu"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-22T15:30:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-11-22T15:30:00Z"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void shouldReturnProblemDetail400WithFieldErrorsWhenRegisterRequestIsInvalid() throws Exception {
		String invalidJson = """
				{
				  "nome": "A",
				  "email": "email-invalido",
				  "senha": "fraca"
				}
				""";

		mockMvc.perform(post("/test-auth-dto/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("email", "nome", "senha")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message", hasItem("formato de e-mail invalido")));
	}

	@RestController
	@RequestMapping("/test-auth-dto")
	static class DummyRegisterController {

		@PostMapping("/register")
		@ResponseStatus(HttpStatus.CREATED)
		AuthUserResponse register(@Valid @RequestBody RegisterRequest request) {
			Instant fixedTimestamp = Instant.parse("2026-11-22T15:30:00Z");
			return new AuthUserResponse(
					UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
					request.nome(),
					request.email(),
					StatusUsuario.ATIVO,
					fixedTimestamp,
					fixedTimestamp
			);
		}
	}
}
