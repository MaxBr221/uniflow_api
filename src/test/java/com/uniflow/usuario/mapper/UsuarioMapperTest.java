package com.uniflow.usuario.mapper;

import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioMapperTest {

	private final UsuarioMapper usuarioMapper = new UsuarioMapper();

	@Test
	void shouldMapUsuarioToPublicUsuarioResponseWithoutSenhaHash() {
		UUID id = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-11-22T10:00:00Z");
		Instant updatedAt = Instant.parse("2026-11-25T12:00:00Z");
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(id);
		usuario.setCreatedAt(createdAt);
		usuario.setUpdatedAt(updatedAt);

		UsuarioResponse response = usuarioMapper.toResponse(usuario);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.nome()).isEqualTo("Ana Silva");
		assertThat(response.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.updatedAt()).isEqualTo(updatedAt);
	}

	@Test
	void shouldRejectNullUsuario() {
		assertThatThrownBy(() -> usuarioMapper.toResponse(null))
				.isInstanceOf(NullPointerException.class);
	}
}
