package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload opcional para encerramento de sessao em {@code POST /api/v1/auth/logout}.
 * <p>
 * Quando {@code refreshToken} e informado, apenas a sessao correspondente (pertencente ao usuario autenticado)
 * e revogada; quando omitido, todos os refresh tokens ativos do usuario autenticado sao revogados.
 */
@Schema(description = "Dados opcionais para revogacao de refresh token especifico no logout")
public record LogoutRequest(
		@Schema(
				description = "Refresh token opaco da sessao a revogar; se omitido, todas as sessoes ativas do usuario autenticado sao revogadas",
				example = "rft_01J...",
				maxLength = 512
		)
		@Pattern(regexp = ".*\\S.*", message = "refreshToken nao deve estar em branco")
		@Size(max = 512, message = "refreshToken deve ter no maximo 512 caracteres")
		String refreshToken
) {

	public LogoutRequest {
		refreshToken = refreshToken == null ? null : refreshToken.trim();
	}

	@Override
	public String toString() {
		return "LogoutRequest[refreshToken=***REDACTED***]";
	}
}
