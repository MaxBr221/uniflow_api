package com.uniflow.usuario.mapper;

import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Conversoes explicitas entre {@link Usuario} e DTOs de perfil do modulo {@code usuario}.
 */
@Component
public class UsuarioMapper {

	public UsuarioResponse toResponse(Usuario usuario) {
		Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getStatus(),
				usuario.getCreatedAt(),
				usuario.getUpdatedAt()
		);
	}
}
