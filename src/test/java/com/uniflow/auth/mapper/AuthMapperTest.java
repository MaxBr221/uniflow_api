package com.uniflow.auth.mapper;

import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthMapperTest {

	private final AuthMapper authMapper = new AuthMapper();

	@Test
	void shouldMapUsuarioToPublicAuthUserResponseWithoutPasswordHash() {
		UUID id = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-11-25T10:00:00Z");
		Instant updatedAt = Instant.parse("2026-11-25T10:05:00Z");

		Usuario usuario = new Usuario("  Ana Silva  ", "  ANA@UniFlow.EDU  ", "$2a$10$hashSecreto");
		usuario.setId(id);
		usuario.setStatus(StatusUsuario.ATIVO);
		usuario.setCreatedAt(createdAt);
		usuario.setUpdatedAt(updatedAt);

		AuthUserResponse response = authMapper.toUserResponse(usuario);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.updatedAt()).isEqualTo(updatedAt);
	}

	@Test
	void shouldRejectNullUsuarioInMapper() {
		assertThatThrownBy(() -> authMapper.toUserResponse(null))
				.isInstanceOf(NullPointerException.class);
	}
}
