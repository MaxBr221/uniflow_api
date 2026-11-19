package com.uniflow;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DependenciesSmokeTest {

	private record SamplePayload(@NotBlank String nome) {
	}

	@Test
	void beanValidationWorksOnClasspath() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();
			assertThat(validator.validate(new SamplePayload(""))).isNotEmpty();
			assertThat(validator.validate(new SamplePayload("UniFlow"))).isEmpty();
		}
	}

	@Test
	void bcryptAndJwtWorkOnClasspath() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode("Segredo@123");
		assertThat(encoder.matches("Segredo@123", hash)).isTrue();

		SecretKey key = Keys.hmacShaKeyFor("uniflow-test-secret-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("usuario-1")
				.signWith(key)
				.compact();

		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertThat(claims.getSubject()).isEqualTo("usuario-1");
	}
}
