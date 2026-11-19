package com.uniflow.common.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

	@Test
	void shouldMaskBearerTokensJwtAndSensitiveFields() {
		String raw = "Authorization: Bearer secreto_opaco password=MinhaSenha123 {\"senhaHash\":\"$2a$10$hash\",\"refreshToken\":\"ref-abc-123\"} eyJhbGciOiJIUzI1NiJ9.payload123456.signature123456";

		String masked = SensitiveDataMasker.mask(raw);

		assertThat(masked)
				.doesNotContain("secreto_opaco")
				.doesNotContain("MinhaSenha123")
				.doesNotContain("$2a$10$hash")
				.doesNotContain("ref-abc-123")
				.doesNotContain("eyJhbGciOiJIUzI1NiJ9")
				.contains("***REDACTED***")
				.contains("***REDACTED_JWT***");
	}

	@Test
	void shouldMaskStandaloneBearerAndBasicSchemesWithoutTouchingNormalMessages() {
		assertThat(SensitiveDataMasker.mask("Header Bearer tok-xyz-999 recebido"))
				.isEqualTo("Header Bearer ***REDACTED*** recebido");
		assertThat(SensitiveDataMasker.mask("Header Basic dXNlcjpwYXNz recebido"))
				.isEqualTo("Header Basic ***REDACTED*** recebido");
		assertThat(SensitiveDataMasker.mask("Domain exception [AUTHENTICATION_FAILED]: Token invalido"))
				.isEqualTo("Domain exception [AUTHENTICATION_FAILED]: Token invalido");
	}

	@Test
	void shouldSanitizeControlCharactersAgainstLogInjection() {
		assertThat(SensitiveDataMasker.sanitizeLogValue("/api/v1/auth\r\nFAKE_LOG\tENTRY"))
				.isEqualTo("/api/v1/auth_FAKE_LOG_ENTRY");
		assertThat(SensitiveDataMasker.sanitizeLogValue(null)).isEmpty();
	}
}
