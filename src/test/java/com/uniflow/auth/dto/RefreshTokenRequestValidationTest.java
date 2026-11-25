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

class RefreshTokenRequestValidationTest {

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
	void shouldAcceptValidRefreshTokenTrimWhitespaceAndRedactToString() {
		RefreshTokenRequest request = new RefreshTokenRequest("  rft_tokenValido123456  ");

		Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request.refreshToken()).isEqualTo("rft_tokenValido123456");
		assertThat(request.toString())
				.doesNotContain("rft_tokenValido123456")
				.contains("***REDACTED***");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "\t\n"})
	void shouldRejectBlankRefreshToken(String blankToken) {
		RefreshTokenRequest request = new RefreshTokenRequest(blankToken);

		Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("refreshToken");
	}

	@Test
	void shouldRejectNullOrOversizedRefreshTokenAbove512Characters() {
		RefreshTokenRequest nullRequest = new RefreshTokenRequest(null);
		RefreshTokenRequest exactLimitRequest = new RefreshTokenRequest("a".repeat(512));
		RefreshTokenRequest oversizedRequest = new RefreshTokenRequest("a".repeat(513));

		assertThat(validator.validate(nullRequest))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("refreshToken");
		assertThat(validator.validate(exactLimitRequest)).isEmpty();
		assertThat(validator.validate(oversizedRequest))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("refreshToken");
	}
}
