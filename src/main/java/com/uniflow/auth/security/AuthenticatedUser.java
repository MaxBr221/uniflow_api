package com.uniflow.auth.security;

import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Principal autenticado populado no {@code SecurityContext} a partir do access token JWT validado.
 */
public record AuthenticatedUser(
		UUID id,
		String email,
		StatusUsuario status
) implements UserDetails {

	private static final List<GrantedAuthority> DEFAULT_AUTHORITIES =
			List.of(new SimpleGrantedAuthority("ROLE_USER"));

	public AuthenticatedUser {
		Objects.requireNonNull(id, "id nao deve ser nulo");
		Objects.requireNonNull(email, "email nao deve ser nulo");
		Objects.requireNonNull(status, "status nao deve ser nulo");
	}

	public static AuthenticatedUser fromUsuario(Usuario usuario) {
		Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		return new AuthenticatedUser(usuario.getId(), usuario.getEmail(), usuario.getStatus());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return DEFAULT_AUTHORITIES;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != StatusUsuario.BLOQUEADO;
	}

	@Override
	public boolean isEnabled() {
		return status.permiteAutenticacao();
	}
}
