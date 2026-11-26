package com.uniflow.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerWebMvcTest.DummyExceptionController.class)
@Import({GlobalExceptionHandlerWebMvcTest.DummyExceptionController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldHandleResourceNotFoundAs404() throws Exception {
		mockMvc.perform(get("/test-exceptions/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/resource-not-found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
				.andExpect(jsonPath("$.detail").value("Semestre nao localizado"))
				.andExpect(jsonPath("$.instance").value("/test-exceptions/not-found"))
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", empty()));
	}

	@Test
	void shouldHandleConflictAs409() throws Exception {
		mockMvc.perform(get("/test-exceptions/conflict"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/resource-conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflito de recurso"))
				.andExpect(jsonPath("$.detail").value("Email ja cadastrado"))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", empty()));
	}

	@Test
	void shouldHandleBusinessRuleViolationAs422() throws Exception {
		mockMvc.perform(get("/test-exceptions/business"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/business-rule-violation"))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.title").value("Regra de negocio violada"))
				.andExpect(jsonPath("$.detail").value("Data inicial deve ser anterior a final"))
				.andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", empty()));
	}

	@Test
	void shouldHandleUnauthorizedAndForbiddenExceptions() throws Exception {
		mockMvc.perform(get("/test-exceptions/unauthorized"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/authentication-failed"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		mockMvc.perform(get("/test-exceptions/forbidden"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/forbidden-operation"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		mockMvc.perform(get("/test-exceptions/spring-auth"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/authentication-failed"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

		mockMvc.perform(get("/test-exceptions/spring-denied"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/forbidden-operation"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
	}

	@Test
	void shouldHandleMalformedJsonMissingParamAndUnsupportedMethod() throws Exception {
		mockMvc.perform(post("/test-exceptions/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{json-invalido"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/malformed-request-body"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Corpo da requisicao invalido"))
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		mockMvc.perform(get("/test-exceptions/param"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/invalid-request-parameter"))
				.andExpect(jsonPath("$.detail").value("Parametro obrigatorio ausente: termo"))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		mockMvc.perform(delete("/test-exceptions/not-found"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/method-not-allowed"))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		mockMvc.perform(post("/test-exceptions/json-only")
						.contentType(MediaType.TEXT_PLAIN)
						.content("texto puro"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/unsupported-media-type"))
				.andExpect(jsonPath("$.status").value(415))
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
				.andExpect(jsonPath("$.fieldErrors", empty()));
	}

	@Test
	void shouldHandleUnexpectedExceptionWithoutLeakingInternalDetails() throws Exception {
		mockMvc.perform(get("/test-exceptions/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/internal-error"))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.title").value("Erro interno do servidor"))
				.andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado ao processar a requisicao."))
				.andExpect(jsonPath("$.instance").value("/test-exceptions/unexpected"))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", empty()))
				.andExpect(content().string(not(containsString("segredo_sql_interno"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	void shouldSerializeNonEmptyFieldErrorsInProblemDetailsResponse() throws Exception {
		mockMvc.perform(get("/test-exceptions/problem-with-fields"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/validation-error"))
				.andExpect(jsonPath("$.title").value("Dados invalidos"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Um ou mais campos estao invalidos."))
				.andExpect(jsonPath("$.instance").value("/test-exceptions/problem-with-fields"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("titulo"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("nao deve estar em branco"));
	}

	@RestController
	@RequestMapping("/test-exceptions")
	static class DummyExceptionController {

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Semestre nao localizado");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("Email ja cadastrado");
		}

		@GetMapping("/business")
		void business() {
			throw new BusinessRuleException("Data inicial deve ser anterior a final");
		}

		@GetMapping("/unauthorized")
		void unauthorized() {
			throw new AuthenticationFailedException("Token invalido");
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new UnauthorizedOperationException("Sem permissao para o recurso");
		}

		@GetMapping("/spring-auth")
		void springAuth() {
			throw new BadCredentialsException("Credenciais invalidas");
		}

		@GetMapping("/spring-denied")
		void springDenied() {
			throw new AccessDeniedException("Acesso negado");
		}

		@PostMapping("/body")
		void body(@RequestBody Map<String, Object> payload) {
		}

		@PostMapping(value = "/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
		void jsonOnly(@RequestBody Map<String, Object> payload) {
		}

		@GetMapping("/param")
		void param(@RequestParam String termo) {
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("segredo_sql_interno");
		}

		@GetMapping("/problem-with-fields")
		org.springframework.http.ResponseEntity<org.springframework.http.ProblemDetail> problemWithFields() {
			return ProblemDetailsFactory.createResponse(
					org.springframework.http.HttpStatus.BAD_REQUEST,
					"VALIDATION_ERROR",
					"Dados invalidos",
					"Um ou mais campos estao invalidos.",
					"/test-exceptions/problem-with-fields",
					java.util.List.of(new FieldErrorDetail("titulo", "nao deve estar em branco"))
			);
		}
	}
}
