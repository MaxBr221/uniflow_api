package com.uniflow.auth.dto;

import com.uniflow.usuario.enums.StatusUsuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidatorFactory() {
		if (validatorFactory != null) {
			validatorFactory.close();
		}
	}

	@Test
	void shouldAcceptValidRegisterRequestNormalizeNomeAndEmailAndRedactPasswordInToString() {
		RegisterRequest request = new RegisterRequest(
				"  Ana Maria Silva  ",
				"  Ana.Maria@UniFlow.EDU  ",
				"UniFlow@2026"
		);

		Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request.nome()).isEqualTo("Ana Maria Silva");
		assertThat(request.email()).isEqualTo("ana.maria@uniflow.edu");
		assertThat(request.senha()).isEqualTo("UniFlow@2026");
		assertThat(request.toString())
				.doesNotContain("UniFlow@2026")
				.contains("***REDACTED***")
				.contains("ana.maria@uniflow.edu");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "A", " A "})
	void shouldRejectBlankOrTooShortNome(String invalidNome) {
		RegisterRequest request = new RegisterRequest(invalidNome, "ana@uniflow.edu", "UniFlow@2026");

		Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("nome");
	}

	@Test
	void shouldRejectNullNomeAndNomeExceedingMaxLength() {
		RegisterRequest nullNome = new RegisterRequest(null, "ana@uniflow.edu", "UniFlow@2026");
		RegisterRequest oversizedNome = new RegisterRequest("A".repeat(151), "ana@uniflow.edu", "UniFlow@2026");

		assertThat(validator.validate(nullNome))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("nome");
		assertThat(validator.validate(oversizedNome))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("nome");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "invalido", "sem-arroba.com", "usuario@", "@uniflow.edu", "ana silva@uniflow.edu"})
	void shouldRejectBlankOrInvalidEmail(String invalidEmail) {
		RegisterRequest request = new RegisterRequest("Ana Silva", invalidEmail, "UniFlow@2026");

		Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}

	@Test
	void shouldRejectNullEmailAndEmailExceedingMaxLength() {
		RegisterRequest nullEmail = new RegisterRequest("Ana Silva", null, "UniFlow@2026");
		String oversizedEmail = "a".repeat(245) + "@uniflow.edu"; // 257 chars > 255
		RegisterRequest longEmail = new RegisterRequest("Ana Silva", oversizedEmail, "UniFlow@2026");

		assertThat(validator.validate(nullEmail))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
		assertThat(validator.validate(longEmail))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"        ",
			"Aa1@567",
			"uniflow@2026",
			"UNIFLOW@2026",
			"UniFlow@Abcd",
			"UniFlow2026",
			"Uni Flow@2026",
			" UniFlow@2026"
	})
	void shouldRejectWeakOrMalformedPassword(String weakPassword) {
		RegisterRequest request = new RegisterRequest("Ana Silva", "ana@uniflow.edu", weakPassword);

		Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
	}

	@Test
	void shouldRejectNullOrOversizedPasswordAbove72Characters() {
		RegisterRequest nullPassword = new RegisterRequest("Ana Silva", "ana@uniflow.edu", null);
		String password73Chars = "Aa1@" + "x".repeat(69);
		RegisterRequest oversizedPassword = new RegisterRequest("Ana Silva", "ana@uniflow.edu", password73Chars);

		assertThat(validator.validate(nullPassword))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
		assertThat(validator.validate(oversizedPassword))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
	}

	@Test
	void shouldExposeOnlyPublicFieldsInAuthUserResponseWithoutPasswordHash() {
		UUID id = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-11-22T15:30:00Z");
		Instant updatedAt = Instant.parse("2026-11-22T15:35:00Z");

		AuthUserResponse response = new AuthUserResponse(
				id,
				"Ana Silva",
				"ana.silva@uniflow.edu",
				StatusUsuario.ATIVO,
				createdAt,
				updatedAt
		);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.updatedAt()).isEqualTo(updatedAt);
		assertThat(AuthUserResponse.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.containsExactly("id", "nome", "email", "status", "createdAt", "updatedAt")
				.doesNotContain("senha", "senhaHash", "password", "passwordHash");
	}
}
