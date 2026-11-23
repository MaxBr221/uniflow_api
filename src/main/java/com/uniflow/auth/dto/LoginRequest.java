package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Payload de entrada para autenticacao de estudante com e-mail e senha.
 */
@Schema(description = "Credenciais de acesso para autenticacao do estudante")
public record LoginRequest(
		@Schema(description = "E-mail cadastrado do estudante (normalizado em lowercase)", example = "ana.silva@uniflow.edu", maxLength = 255)
		@NotBlank(message = "e-mail e obrigatorio")
		@Email(message = "formato de e-mail invalido")
		@Size(max = 255, message = "e-mail deve ter no maximo 255 caracteres")
		String email,

		@Schema(description = "Senha da conta do estudante", example = "UniFlow@2026", maxLength = 72, format = "password")
		@NotBlank(message = "senha e obrigatoria")
		@Size(max = 72, message = "senha deve ter no maximo 72 caracteres")
		String senha
) {

	public LoginRequest {
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", senha=***REDACTED***]";
	}
}
