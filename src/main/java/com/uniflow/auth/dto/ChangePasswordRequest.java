package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para alteracao de senha do usuario autenticado ({@code PATCH /api/v1/auth/password}).
 */
@Schema(description = "Dados necessarios para alteracao de senha da conta autenticada")
public record ChangePasswordRequest(
		@Schema(
				description = "Senha atual da conta autenticada",
				example = "UniFlow@2026",
				maxLength = PasswordPolicy.MAX_LENGTH,
				format = "password"
		)
		@NotBlank(message = "senhaAtual e obrigatoria")
		@Size(max = PasswordPolicy.MAX_LENGTH, message = "senhaAtual deve ter no maximo 72 caracteres")
		String senhaAtual,

		@Schema(
				description = "Nova senha forte (8 a 72 caracteres, contendo letra maiuscula, minuscula, numero e caractere especial, sem espacos)",
				example = "UniFlow@2027",
				minLength = PasswordPolicy.MIN_LENGTH,
				maxLength = PasswordPolicy.MAX_LENGTH,
				format = "password"
		)
		@NotBlank(message = "novaSenha e obrigatoria")
		@Size(
				min = PasswordPolicy.MIN_LENGTH,
				max = PasswordPolicy.MAX_LENGTH,
				message = "novaSenha deve ter entre 8 e 72 caracteres"
		)
		@Pattern(
				regexp = PasswordPolicy.STRONG_PASSWORD_REGEX,
				message = "novaSenha deve conter letra maiuscula, minuscula, numero, caractere especial e nao possuir espacos"
		)
		String novaSenha
) {

	@Override
	public String toString() {
		return "ChangePasswordRequest[senhaAtual=***REDACTED***, novaSenha=***REDACTED***]";
	}
}
