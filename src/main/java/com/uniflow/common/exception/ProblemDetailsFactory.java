package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Fabrica utilitaria para construcao padronizada de respostas de erro no formato
 * RFC 7807 ({@link ProblemDetail}) conforme contrato da API UniFlow.
 */
public final class ProblemDetailsFactory {

	public static final String ERROR_TYPE_BASE_URI = "https://api.uniflow.dev/errors/";
	public static final String DEFAULT_ERROR_CODE = "INTERNAL_ERROR";

	private ProblemDetailsFactory() {
	}

	public static ProblemDetail create(
			HttpStatus status,
			String code,
			String title,
			String detail,
			String instancePath) {
		return create(status, code, title, detail, instancePath, List.of());
	}

	public static ProblemDetail create(
			HttpStatus status,
			String code,
			String title,
			String detail,
			String instancePath,
			List<FieldErrorDetail> fieldErrors) {
		return create(
				status,
				code,
				title,
				detail,
				instancePath,
				fieldErrors,
				Instant.now().truncatedTo(ChronoUnit.SECONDS)
		);
	}

	public static ProblemDetail create(
			HttpStatus status,
			String code,
			String title,
			String detail,
			String instancePath,
			List<FieldErrorDetail> fieldErrors,
			Instant timestamp) {
		Objects.requireNonNull(status, "status must not be null");

		String normalizedCode = normalizeCode(code);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(buildTypeUri(normalizedCode));
		problem.setTitle(title != null && !title.isBlank() ? title : status.getReasonPhrase());
		if (instancePath != null && !instancePath.isBlank()) {
			problem.setInstance(URI.create(instancePath));
		}

		Instant resolvedTimestamp = (timestamp != null ? timestamp : Instant.now()).truncatedTo(ChronoUnit.SECONDS);
		List<FieldErrorDetail> resolvedFieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);

		problem.setProperty("code", normalizedCode);
		problem.setProperty("timestamp", resolvedTimestamp.toString());
		problem.setProperty("fieldErrors", resolvedFieldErrors);
		return problem;
	}

	public static ResponseEntity<ProblemDetail> createResponse(
			HttpStatus status,
			String code,
			String title,
			String detail,
			String instancePath) {
		return createResponse(status, code, title, detail, instancePath, List.of());
	}

	public static ResponseEntity<ProblemDetail> createResponse(
			HttpStatus status,
			String code,
			String title,
			String detail,
			String instancePath,
			List<FieldErrorDetail> fieldErrors) {
		ProblemDetail problem = create(status, code, title, detail, instancePath, fieldErrors);
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	public static URI buildTypeUri(String code) {
		String slug = normalizeCode(code)
				.toLowerCase(Locale.ROOT)
				.replace('_', '-')
				.replaceAll("[^a-z0-9-]+", "-")
				.replaceAll("^-+|-+$", "");
		if (slug.isEmpty()) {
			slug = "internal-error";
		}
		return URI.create(ERROR_TYPE_BASE_URI + slug);
	}

	private static String normalizeCode(String code) {
		if (code == null || code.isBlank()) {
			return DEFAULT_ERROR_CODE;
		}
		return code.trim().toUpperCase(Locale.ROOT);
	}
}
