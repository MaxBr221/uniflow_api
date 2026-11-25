package com.uniflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuracao central do {@link PasswordEncoder} da aplicacao utilizando BCrypt ({@code RNF-005}).
 */
@Configuration
public class PasswordEncoderConfig {

	public static final int DEFAULT_BCRYPT_STRENGTH = 10;
	public static final int MIN_BCRYPT_STRENGTH = 10;
	public static final int MAX_BCRYPT_STRENGTH = 31;

	@Bean
	public PasswordEncoder passwordEncoder(
			@Value("${app.security.password.bcrypt-strength:" + DEFAULT_BCRYPT_STRENGTH + "}") int strength) {
		if (strength < MIN_BCRYPT_STRENGTH || strength > MAX_BCRYPT_STRENGTH) {
			throw new IllegalArgumentException(
					"Fator de custo BCrypt invalido: " + strength
							+ ". Deve estar entre " + MIN_BCRYPT_STRENGTH + " e " + MAX_BCRYPT_STRENGTH + "."
			);
		}
		return new BCryptPasswordEncoder(strength);
	}
}
