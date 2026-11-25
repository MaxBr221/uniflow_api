package com.uniflow.auth.mapper;

import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Conversoes explicitas entre entidades do dominio e DTOs de autenticacao.
 */
@Component
public class AuthMapper {

	/**
	 * Converte {@link Usuario} para {@link AuthUserResponse} sem expor {@code senhaHash}.
	 *
	 * @param usuario entidade persistida do usuario
	 * @return DTO publico com dados seguros do usuario
	 */
	public AuthUserResponse toUserResponse(Usuario usuario) {
		Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		return new AuthUserResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getStatus(),
				usuario.getCreatedAt(),
				usuario.getUpdatedAt()
		);
	}
}
