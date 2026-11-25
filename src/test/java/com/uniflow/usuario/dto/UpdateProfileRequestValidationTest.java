package com.uniflow.usuario.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileRequestValidationTest {

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
	void shouldAcceptPartialOrFullValidProfileUpdateAndNormalizeFields() {
		UpdateProfileRequest onlyName = new UpdateProfileRequest("  Ana Maria Silva  ", null);
		UpdateProfileRequest onlyEmail = new UpdateProfileRequest(null, "  ANA.MARIA@UniFlow.EDU  ");
		UpdateProfileRequest both = new UpdateProfileRequest("  Ana Maria  ", "  ANA@UniFlow.EDU ");

		assertThat(validator.validate(onlyName)).isEmpty();
		assertThat(onlyName.nome()).isEqualTo("Ana Maria Silva");

		assertThat(validator.validate(onlyEmail)).isEmpty();
		assertThat(onlyEmail.email()).isEqualTo("ana.maria@uniflow.edu");

		assertThat(validator.validate(both)).isEmpty();
		assertThat(both.nome()).isEqualTo("Ana Maria");
		assertThat(both.email()).isEqualTo("ana@uniflow.edu");
	}

	@Test
	void shouldRejectWhenNoFieldIsProvided() {
		UpdateProfileRequest empty = new UpdateProfileRequest(null, null);

		Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(empty);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("anyFieldPresent");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "A"})
	void shouldRejectBlankOrShortNome(String invalidNome) {
		UpdateProfileRequest request = new UpdateProfileRequest(invalidNome, null);

		Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("nome");
	}

	@Test
	void shouldRejectOversizedNomeAbove150Characters() {
		UpdateProfileRequest oversized = new UpdateProfileRequest("a".repeat(151), null);

		assertThat(validator.validate(oversized))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("nome");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "email-invalido", "sem-arroba.com", "usuario@", "@uniflow.edu"})
	void shouldRejectBlankOrInvalidEmail(String invalidEmail) {
		UpdateProfileRequest request = new UpdateProfileRequest(null, invalidEmail);

		Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}

	@Test
	void shouldRejectEmailExceeding255Characters() {
		String oversizedEmail = "a".repeat(245) + "@uniflow.edu";
		UpdateProfileRequest request = new UpdateProfileRequest(null, oversizedEmail);

		assertThat(validator.validate(request))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}
}
