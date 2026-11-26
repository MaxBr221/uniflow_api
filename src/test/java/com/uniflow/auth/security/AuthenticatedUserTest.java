package com.uniflow.auth.security;

import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserTest {

	@Test
	void shouldCreatePrincipalFromUsuarioWithoutExposingPasswordAndWithImmutableAuthorities() {
		UUID id = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(id);

		AuthenticatedUser principal = AuthenticatedUser.fromUsuario(usuario);

		assertThat(principal.id()).isEqualTo(id);
		assertThat(principal.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(principal.getUsername()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(principal.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(principal.getPassword()).isNull();
		assertThat(principal.toString()).doesNotContain("$2a$10$hashSecreto");
		assertThat(principal.getAuthorities())
				.hasSize(1)
				.anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_USER")));
		assertThat(principal.isAccountNonExpired()).isTrue();
		assertThat(principal.isCredentialsNonExpired()).isTrue();
		assertThatThrownBy(() -> principal.getAuthorities().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest
	@EnumSource(StatusUsuario.class)
	void shouldAlignEnabledAndLockedFlagsWithStatusUsuarioSemantics(StatusUsuario status) {
		AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "ana@uniflow.edu", status);

		assertThat(principal.isEnabled()).isEqualTo(status == StatusUsuario.ATIVO);
		assertThat(principal.isAccountNonLocked()).isEqualTo(status != StatusUsuario.BLOQUEADO);
	}

	@Test
	void shouldFailFastWhenRequiredFieldsAreNull() {
		UUID id = UUID.randomUUID();
		assertThatThrownBy(() -> AuthenticatedUser.fromUsuario(null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedUser(null, "ana@uniflow.edu", StatusUsuario.ATIVO))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedUser(id, null, StatusUsuario.ATIVO))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new AuthenticatedUser(id, "ana@uniflow.edu", null))
				.isInstanceOf(NullPointerException.class);
	}
}
