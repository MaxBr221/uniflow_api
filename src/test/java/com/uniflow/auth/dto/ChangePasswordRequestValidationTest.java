package com.uniflow.auth.dto;

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

class ChangePasswordRequestValidationTest {

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
	void shouldAcceptValidPasswordsPreserveCurrentPasswordExactCharsAndRedactToString() {
		String senhaAtualComEspacos = "  UniFlow@2026  ";
		String novaSenhaValida = "UniFlow@2027";
		ChangePasswordRequest request = new ChangePasswordRequest(senhaAtualComEspacos, novaSenhaValida);

		assertThat(validator.validate(request)).isEmpty();
		assertThat(request.senhaAtual()).isEqualTo(senhaAtualComEspacos);
		assertThat(request.novaSenha()).isEqualTo(novaSenhaValida);
		assertThat(request.toString())
				.doesNotContain("UniFlow@2026")
				.doesNotContain("UniFlow@2027")
				.contains("senhaAtual=***REDACTED***")
				.contains("novaSenha=***REDACTED***");
	}

	@Test
	void shouldAcceptBoundaryStrongPasswordsWith8And72Characters() {
		String minStrong = "Aa1@5678";
		String maxStrong = "Aa1@" + "x".repeat(68);

		assertThat(minStrong).hasSize(8);
		assertThat(maxStrong).hasSize(72);
		assertThat(validator.validate(new ChangePasswordRequest("Atual@2026", minStrong))).isEmpty();
		assertThat(validator.validate(new ChangePasswordRequest("Atual@2026", maxStrong))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "\t\n"})
	void shouldRejectBlankSenhaAtual(String invalidCurrentPassword) {
		ChangePasswordRequest request = new ChangePasswordRequest(invalidCurrentPassword, "UniFlow@2027");

		Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senhaAtual");
	}

	@Test
	void shouldRejectNullOrOversizedSenhaAtualAbove72Characters() {
		assertThat(validator.validate(new ChangePasswordRequest(null, "UniFlow@2027")))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senhaAtual");
		assertThat(validator.validate(new ChangePasswordRequest("x".repeat(73), "UniFlow@2027")))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senhaAtual");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"        ",
			"Aa1@567",
			"uniflow@2027",
			"UNIFLOW@2027",
			"UniFlow@Abcd",
			"UniFlow2027",
			"Uni Flow@2027",
			" UniFlow@2027",
			"UniFlow@2027 "
	})
	void shouldRejectWeakOrWhitespaceNovaSenha(String weakNewPassword) {
		ChangePasswordRequest request = new ChangePasswordRequest("UniFlow@2026", weakNewPassword);

		Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("novaSenha");
	}

	@Test
	void shouldRejectNullOrOversizedNovaSenhaAbove72Characters() {
		assertThat(validator.validate(new ChangePasswordRequest("UniFlow@2026", null)))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("novaSenha");
		assertThat(validator.validate(new ChangePasswordRequest("UniFlow@2026", "Aa1@" + "x".repeat(69))))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("novaSenha");
	}
}
