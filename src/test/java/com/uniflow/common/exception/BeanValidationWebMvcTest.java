package com.uniflow.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BeanValidationWebMvcTest.ValidationDummyController.class)
@Import({
		BeanValidationWebMvcTest.ValidationDummyController.class,
		BeanValidationWebMvcTest.ValidatedDummyService.class,
		GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BeanValidationWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldMapMethodArgumentNotValidExceptionIncludingNestedFieldsToProblemDetail() throws Exception {
		String invalidPayload = """
				{
				  "titulo": "",
				  "cargaHoraria": 0,
				  "contato": {
				    "email": "invalido"
				  }
				}
				""";

		mockMvc.perform(post("/test-validation/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidPayload))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.title").value("Dados invalidos"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Um ou mais campos estao invalidos."))
				.andExpect(jsonPath("$.instance").value("/test-validation/body"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", hasSize(3)))
				.andExpect(jsonPath("$.fieldErrors[*].field", containsInRelativeOrder("cargaHoraria", "contato.email", "titulo")))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("deve ser maior ou igual a 1"))
				.andExpect(jsonPath("$.fieldErrors[1].message").value("formato de e-mail invalido"));
	}

	@Test
	void shouldMapHandlerMethodValidationExceptionOnRequestParamAndPathVariableToProblemDetail() throws Exception {
		mockMvc.perform(get("/test-validation/items/ab")
						.param("pagina", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.title").value("Dados invalidos"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.instance").value("/test-validation/items/ab"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)))
				.andExpect(jsonPath("$.fieldErrors[*].field", containsInRelativeOrder("codigo", "pagina")));
	}

	@Test
	void shouldMapConstraintViolationExceptionFromValidatedServiceToProblemDetail() throws Exception {
		mockMvc.perform(post("/test-validation/service-body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "titulo": "Algoritmos",
								  "cargaHoraria": 60,
								  "contato": {
								    "email": "nao-eh-email"
								  }
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItem("contato.email")))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("formato de e-mail invalido"));
	}

	@Test
	void shouldMapParameterConstraintViolationExceptionFromValidatedServiceToProblemDetail() throws Exception {
		mockMvc.perform(get("/test-validation/service-param")
						.param("termo", " "))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("termo"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("termo obrigatorio"));
	}

	@RestController
	@RequestMapping("/test-validation")
	static class ValidationDummyController {

		private final ValidatedDummyService service;

		ValidationDummyController(ValidatedDummyService service) {
			this.service = service;
		}

		@PostMapping("/body")
		void validateBody(@Valid @RequestBody DummyCreateRequest request) {
		}

		@GetMapping("/items/{codigo}")
		void validateParameters(
				@PathVariable @Size(min = 3, message = "codigo deve ter ao menos 3 caracteres") String codigo,
				@RequestParam @Min(value = 1, message = "pagina deve ser no minimo 1") int pagina) {
		}

		@PostMapping("/service-body")
		void validateViaServiceDto(@RequestBody DummyCreateRequest request) {
			service.processarDto(request);
		}

		@GetMapping("/service-param")
		void validateViaServiceParam(@RequestParam String termo) {
			service.buscarPorTermo(termo);
		}
	}

	@Service
	@Validated
	static class ValidatedDummyService {

		void processarDto(@Valid DummyCreateRequest request) {
		}

		void buscarPorTermo(@NotBlank(message = "termo obrigatorio") String termo) {
		}
	}

	record DummyCreateRequest(
			@NotBlank(message = "titulo e obrigatorio")
			String titulo,

			@NotNull
			@Min(value = 1, message = "deve ser maior ou igual a 1")
			Integer cargaHoraria,

			@Valid
			@NotNull
			DummyContact contato
	) {
	}

	record DummyContact(
			@NotBlank
			@Email(message = "formato de e-mail invalido")
			String email
	) {
	}
}
