package com.uniflow.usuario.entity;

import com.uniflow.usuario.enums.StatusUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

	@Test
	void shouldNormalizeNomeAndEmailAndDefaultStatusToAtivo() {
		Usuario usuario = new Usuario("  Ana Silva  ", "  Ana.Silva@UniFlow.DEV  ", "$2a$10$hashExemplo");

		assertThat(usuario.getNome()).isEqualTo("Ana Silva");
		assertThat(usuario.getEmail()).isEqualTo("ana.silva@uniflow.dev");
		assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$hashExemplo");
		assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(usuario.podeAutenticar()).isTrue();
	}

	@Test
	void shouldPopulateAndUpdateUtcAuditTimestampsViaJpaLifecycleCallbacks() {
		Usuario usuario = new Usuario("Carlos Lima", "CARLOS@DOMINIO.COM", "$2a$10$segredoHash");
		Instant beforePersist = Instant.now();

		usuario.onPrePersist();

		Instant createdAt = usuario.getCreatedAt();
		assertThat(createdAt).isNotNull().isAfterOrEqualTo(beforePersist);
		assertThat(usuario.getUpdatedAt()).isEqualTo(createdAt);
		assertThat(usuario.getEmail()).isEqualTo("carlos@dominio.com");

		usuario.setEmail("  Novo.Email@UniFlow.Dev ");
		usuario.onPreUpdate();

		assertThat(usuario.getEmail()).isEqualTo("novo.email@uniflow.dev");
		assertThat(usuario.getCreatedAt()).isEqualTo(createdAt);
		assertThat(usuario.getUpdatedAt()).isAfterOrEqualTo(createdAt);
	}

	@Test
	void shouldNeverLeakSenhaHashInToStringAndRespectEntityMappings() throws NoSuchFieldException {
		Usuario usuario = new Usuario("Maria Souza", "maria@uniflow.dev", "$2a$10$hashSuperSecreto");
		usuario.setId(UUID.randomUUID());

		assertThat(usuario.toString()).doesNotContain("$2a$10$hashSuperSecreto").doesNotContain("senhaHash");

		Table table = Usuario.class.getAnnotation(Table.class);
		assertThat(Usuario.class.isAnnotationPresent(Entity.class)).isTrue();
		assertThat(table).isNotNull();
		assertThat(table.name()).isEqualTo("usuarios");

		Enumerated enumerated = Usuario.class.getDeclaredField("status").getAnnotation(Enumerated.class);
		assertThat(enumerated).isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);

		Column statusCol = Usuario.class.getDeclaredField("status").getAnnotation(Column.class);
		assertThat(statusCol).isNotNull();
		assertThat(statusCol.length()).isEqualTo(40);
	}
}
