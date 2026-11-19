package com.uniflow.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsFactoryTest {

	@Test
	void shouldCreateProblemDetailWithAllContractFieldsAndFieldErrors() {
		Instant fixedTimestamp = Instant.parse("2026-11-16T22:00:00.789Z");
		List<FieldErrorDetail> fieldErrors = List.of(
				new FieldErrorDetail("titulo", "nao deve estar em branco"),
				new FieldErrorDetail("cargaHoraria", "deve ser maior que zero")
		);

		ProblemDetail problem = ProblemDetailsFactory.create(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Dados invalidos",
				"Um ou mais campos estao invalidos.",
				"/api/v1/atividades",
				fieldErrors,
				fixedTimestamp
		);

		assertThat(problem.getType()).isEqualTo(URI.create("https://api.uniflow.dev/errors/validation-error"));
		assertThat(problem.getTitle()).isEqualTo("Dados invalidos");
		assertThat(problem.getStatus()).isEqualTo(400);
		assertThat(problem.getDetail()).isEqualTo("Um ou mais campos estao invalidos.");
		assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/atividades"));
		assertThat(problem.getProperties())
				.containsEntry("code", "VALIDATION_ERROR")
				.containsEntry("timestamp", "2026-11-16T22:00:00Z");

		@SuppressWarnings("unchecked")
		List<FieldErrorDetail> storedErrors = (List<FieldErrorDetail>) problem.getProperties().get("fieldErrors");
		assertThat(storedErrors)
				.containsExactly(
						new FieldErrorDetail("titulo", "nao deve estar em branco"),
						new FieldErrorDetail("cargaHoraria", "deve ser maior que zero")
				);
	}

	@Test
	void shouldNormalizeDefaultsWhenCodeTitleOrFieldErrorsAreBlankOrNull() {
		ResponseEntity<ProblemDetail> response = ProblemDetailsFactory.createResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				" ",
				null,
				"Falha interna.",
				null
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

		ProblemDetail body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getType()).isEqualTo(URI.create("https://api.uniflow.dev/errors/internal-error"));
		assertThat(body.getTitle()).isEqualTo("Internal Server Error");
		assertThat(body.getInstance()).isNull();
		assertThat(body.getProperties())
				.containsEntry("code", "INTERNAL_ERROR")
				.containsKey("timestamp");
		assertThat(body.getProperties().get("timestamp").toString())
				.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");
		assertThat((List<?>) body.getProperties().get("fieldErrors")).isEmpty();
	}
}
