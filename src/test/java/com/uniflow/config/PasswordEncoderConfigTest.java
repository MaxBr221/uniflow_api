package com.uniflow.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncoderConfigTest {

	private final PasswordEncoderConfig config = new PasswordEncoderConfig();

	@Test
	void shouldCreateBCryptPasswordEncoderWithSaltedNonReversibleHashesAtDefaultStrength() {
		PasswordEncoder encoder = config.passwordEncoder(PasswordEncoderConfig.DEFAULT_BCRYPT_STRENGTH);

		assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

		String rawPassword = "UniFlow@2026";
		String hashA = encoder.encode(rawPassword);
		String hashB = encoder.encode(rawPassword);

		assertThat(hashA)
				.isNotEqualTo(rawPassword)
				.isNotEqualTo(hashB)
				.startsWith("$2a$10$")
				.hasSizeLessThanOrEqualTo(255);
		assertThat(encoder.matches(rawPassword, hashA)).isTrue();
		assertThat(encoder.matches(rawPassword, hashB)).isTrue();
		assertThat(encoder.matches("SenhaErrada@2026", hashA)).isFalse();
	}

	@Test
	void shouldAllowConfiguredStrongerBCryptCostFactor() {
		PasswordEncoder encoder = config.passwordEncoder(12);

		String hash = encoder.encode("UniFlow@2026");

		assertThat(hash).startsWith("$2a$12$");
		assertThat(encoder.matches("UniFlow@2026", hash)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 4, 9, 32})
	void shouldRejectWeakOrOutOfRangeBCryptCostFactor(int invalidStrength) {
		assertThatThrownBy(() -> config.passwordEncoder(invalidStrength))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Fator de custo BCrypt invalido");
	}
}
