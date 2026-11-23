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

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

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
	void shouldAcceptValidLoginRequestNormalizeEmailPreservePasswordAndRedactToString() {
		LoginRequest request = new LoginRequest("  Ana.Silva@UniFlow.EDU  ", " senhaComEspaco123 ");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(request.senha()).isEqualTo(" senhaComEspaco123 ");
		assertThat(request.toString())
				.doesNotContain("senhaComEspaco123")
				.contains("***REDACTED***")
				.contains("ana.silva@uniflow.edu");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "invalido", "sem-arroba.com", "usuario@", "@uniflow.edu", "ana silva@uniflow.edu"})
	void shouldRejectBlankOrInvalidEmail(String invalidEmail) {
		LoginRequest request = new LoginRequest(invalidEmail, "UniFlow@2026");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}

	@Test
	void shouldRejectNullEmailAndEmailExceedingMaxLength() {
		LoginRequest nullEmail = new LoginRequest(null, "UniFlow@2026");
		String oversizedEmail = "a".repeat(245) + "@uniflow.edu";
		LoginRequest longEmail = new LoginRequest(oversizedEmail, "UniFlow@2026");

		assertThat(validator.validate(nullEmail))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
		assertThat(validator.validate(longEmail))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("email");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankPassword(String blankPassword) {
		LoginRequest request = new LoginRequest("ana@uniflow.edu", blankPassword);

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
	}

	@Test
	void shouldRejectNullOrOversizedPasswordAbove72Characters() {
		LoginRequest nullPassword = new LoginRequest("ana@uniflow.edu", null);
		LoginRequest oversizedPassword = new LoginRequest("ana@uniflow.edu", "x".repeat(73));

		assertThat(validator.validate(nullPassword))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
		assertThat(validator.validate(oversizedPassword))
				.extracting(v -> v.getPropertyPath().toString())
				.contains("senha");
	}

	@Test
	void shouldBuildBearerAuthTokenResponseAndRedactSensitiveTokensInToString() {
		Instant now = Instant.parse("2026-11-23T10:00:00Z");
		AuthUserResponse user = new AuthUserResponse(
				UUID.randomUUID(),
				"Ana Silva",
				"ana.silva@uniflow.edu",
				StatusUsuario.ATIVO,
				now,
				now
		);

		AuthTokenResponse response = AuthTokenResponse.bearer(
				"jwt.access.secreto",
				"refresh.token.secreto",
				900L,
				user
		);

		assertThat(response.accessToken()).isEqualTo("jwt.access.secreto");
		assertThat(response.refreshToken()).isEqualTo("refresh.token.secreto");
		assertThat(response.expiresIn()).isEqualTo(900L);
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.usuario()).isEqualTo(user);
		assertThat(AuthTokenResponse.class.getRecordComponents())
				.extracting(RecordComponent::getName)
				.containsExactly("accessToken", "refreshToken", "expiresIn", "tokenType", "usuario");
		assertThat(response.toString())
				.doesNotContain("jwt.access.secreto")
				.doesNotContain("refresh.token.secreto")
				.contains("***REDACTED***")
				.contains("tokenType=Bearer")
				.contains("expiresIn=900");
	}
}
