package com.uniflow.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Payload de entrada para atualizacao parcial dos dados pessoais permitidos do usuario autenticado.
 * <p>
 * Campos sensiveis como {@code id}, {@code status} e {@code senhaHash} sao propositalmente ausentes
 * deste contrato e nunca podem ser alterados pelo endpoint de perfil.
 */
@Schema(description = "Dados pessoais permitidos para atualizacao do perfil do usuario autenticado")
public record UpdateProfileRequest(
		@Schema(description = "Novo nome completo do estudante (opcional na requisicao PATCH)", example = "Ana Maria Silva", minLength = 2, maxLength = 150)
		@Pattern(regexp = ".*\\S.*", message = "nome nao deve estar em branco")
		@Size(min = 2, max = 150, message = "nome deve ter entre 2 e 150 caracteres")
		String nome,

		@Schema(description = "Novo e-mail do estudante (opcional na requisicao PATCH, normalizado em lowercase)", example = "ana.maria@uniflow.edu", maxLength = 255)
		@Pattern(regexp = ".*\\S.*", message = "e-mail nao deve estar em branco")
		@Email(message = "formato de e-mail invalido")
		@Size(max = 255, message = "e-mail deve ter no maximo 255 caracteres")
		String email
) {

	public UpdateProfileRequest {
		nome = nome == null ? null : nome.trim();
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	@JsonIgnore
	@AssertTrue(message = "informe ao menos um campo permitido para atualizacao (nome ou email)")
	public boolean isAnyFieldPresent() {
		return nome != null || email != null;
	}
}
