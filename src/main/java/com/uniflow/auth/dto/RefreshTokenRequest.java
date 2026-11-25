package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para renovacao de sessao utilizando refresh token opaco com rotacao.
 */
@Schema(description = "Dados necessarios para renovacao de sessao com refresh token")
public record RefreshTokenRequest(
		@Schema(
				description = "Refresh token opaco emitido no login ou na renovacao anterior",
				example = "rft_01J...",
				maxLength = 512
		)
		@NotBlank(message = "refreshToken e obrigatorio")
		@Size(max = 512, message = "refreshToken deve ter no maximo 512 caracteres")
		String refreshToken
) {

	public RefreshTokenRequest {
		refreshToken = refreshToken == null ? null : refreshToken.trim();
	}

	@Override
	public String toString() {
		return "RefreshTokenRequest[refreshToken=***REDACTED***]";
	}
}
