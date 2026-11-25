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

class LogoutRequestValidationTest {

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
	void shouldAcceptNullOrValidRefreshTokenAndRedactToString() {
		LogoutRequest withoutToken = new LogoutRequest(null);
		LogoutRequest withToken = new LogoutRequest("  rft_sessaoAtual123  ");

		assertThat(validator.validate(withoutToken)).isEmpty();
		assertThat(validator.validate(withToken)).isEmpty();
		assertThat(withToken.refreshToken()).isEqualTo("rft_sessaoAtual123");
		assertThat(withToken.toString())
				.doesNotContain("rft_sessaoAtual123")
				.contains("***REDACTED***");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "\t\n"})
	void shouldRejectExplicitBlankRefreshToken(String blankToken) {
		LogoutRequest request = new LogoutRequest(blankToken);

		Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("refreshToken");
	}

	@Test
	void shouldRejectOversizedRefreshTokenAbove512CharactersAndAcceptBoundary() {
		assertThat(validator.validate(new LogoutRequest("a".repeat(512)))).isEmpty();
		assertThat(validator.validate(new LogoutRequest("a".repeat(513))))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("refreshToken");
	}
}
