package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Payload de entrada para cadastro de conta de estudante.
 */
@Schema(description = "Dados necessarios para cadastro de uma nova conta de estudante")
public record RegisterRequest(
		@Schema(description = "Nome completo do estudante", example = "Ana Silva", minLength = 2, maxLength = 150)
		@NotBlank(message = "nome e obrigatorio")
		@Size(min = 2, max = 150, message = "nome deve ter entre 2 e 150 caracteres")
		String nome,

		@Schema(description = "E-mail do estudante (normalizado em lowercase)", example = "ana.silva@uniflow.edu", maxLength = 255)
		@NotBlank(message = "e-mail e obrigatorio")
		@Email(message = "formato de e-mail invalido")
		@Size(max = 255, message = "e-mail deve ter no maximo 255 caracteres")
		String email,

		@Schema(
				description = "Senha forte (8 a 72 caracteres, contendo letra maiuscula, minuscula, numero e caractere especial, sem espacos)",
				example = "UniFlow@2026",
				minLength = 8,
				maxLength = 72,
				format = "password"
		)
		@NotBlank(message = "senha e obrigatoria")
		@Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
				message = "senha deve conter letra maiuscula, minuscula, numero, caractere especial e nao possuir espacos"
		)
		String senha
) {

	public RegisterRequest {
		nome = nome == null ? null : nome.trim();
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return "RegisterRequest[nome=" + nome + ", email=" + email + ", senha=***REDACTED***]";
	}
}
