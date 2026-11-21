package com.uniflow.usuario.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusUsuarioTest {

	@Test
	void shouldContainAllExpectedUserStatusValues() {
		assertThat(StatusUsuario.values()).containsExactly(
				StatusUsuario.ATIVO,
				StatusUsuario.INATIVO,
				StatusUsuario.BLOQUEADO,
				StatusUsuario.EXCLUIDO
		);
	}

	@Test
	void shouldAllowAuthenticationOnlyWhenActive() {
		assertThat(StatusUsuario.ATIVO.permiteAutenticacao()).isTrue();
		assertThat(StatusUsuario.INATIVO.permiteAutenticacao()).isFalse();
		assertThat(StatusUsuario.BLOQUEADO.permiteAutenticacao()).isFalse();
		assertThat(StatusUsuario.EXCLUIDO.permiteAutenticacao()).isFalse();
	}
}
