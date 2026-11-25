package com.uniflow.auth.entity;

import com.uniflow.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

	@Test
	void shouldEvaluateExpirationActiveStateRevocationAndRotationCorrectly() {
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(UUID.randomUUID());

		Instant createdAt = Instant.parse("2026-11-25T10:00:00Z");
		Instant expiresAt = Instant.parse("2026-12-02T10:00:00Z");
		RefreshToken token = new RefreshToken(usuario, "a".repeat(64), expiresAt, createdAt);
		token.setId(UUID.randomUUID());

		assertThat(token.isExpired(Instant.parse("2026-12-02T09:59:59Z"))).isFalse();
		assertThat(token.isActive(Instant.parse("2026-12-02T09:59:59Z"))).isTrue();
		assertThat(token.isExpired(expiresAt)).isTrue();
		assertThat(token.isActive(expiresAt)).isFalse();

		UUID replacementId = UUID.randomUUID();
		Instant rotatedAt = Instant.parse("2026-11-26T10:00:00Z");
		token.markRotated(replacementId, rotatedAt);

		assertThat(token.isRevoked()).isTrue();
		assertThat(token.getRevokedAt()).isEqualTo(rotatedAt);
		assertThat(token.getReplacedByTokenId()).isEqualTo(replacementId);
		assertThat(token.isActive(Instant.parse("2026-11-26T10:01:00Z"))).isFalse();

		// Idempotente ao revogar novamente: preserva revokedAt original
		token.revoke(Instant.parse("2026-11-27T10:00:00Z"));
		assertThat(token.getRevokedAt()).isEqualTo(rotatedAt);
	}

	@Test
	void shouldValidateInvariantsAndNeverExposeTokenHashInToString() {
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		Instant now = Instant.parse("2026-11-25T10:00:00Z");
		String hash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

		RefreshToken token = new RefreshToken(usuario, hash, now.plusSeconds(3600), now);
		UUID id = UUID.randomUUID();
		token.setId(id);

		assertThat(token.toString()).doesNotContain(hash);
		assertThat(token.toString()).contains(id.toString());

		assertThatThrownBy(() -> token.markRotated(id, now))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RefreshToken(usuario, "   ", now.plusSeconds(60), now))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
