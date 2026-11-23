package com.uniflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta de autenticacao emitida em login e renovacao de sessao com JWT e refresh token.
 */
@Schema(description = "Tokens de autenticacao emitidos e dados publicos do usuario autenticado")
public record AuthTokenResponse(
		@Schema(description = "Access token JWT para autorizacao nas rotas protegidas", example = "eyJhbGciOiJIUzI1NiJ9...")
		String accessToken,

		@Schema(description = "Refresh token opaco utilizado para rotacao de sessao", example = "rft_01J...")
		String refreshToken,

		@Schema(description = "Tempo de expiracao do access token em segundos", example = "900")
		long expiresIn,

		@Schema(description = "Tipo do token no cabecalho Authorization", example = "Bearer")
		String tokenType,

		@Schema(description = "Dados publicos do usuario autenticado")
		AuthUserResponse usuario
) {

	public static final String TOKEN_TYPE_BEARER = "Bearer";

	public static AuthTokenResponse bearer(
			String accessToken,
			String refreshToken,
			long expiresInSeconds,
			AuthUserResponse usuario) {
		return new AuthTokenResponse(accessToken, refreshToken, expiresInSeconds, TOKEN_TYPE_BEARER, usuario);
	}

	@Override
	public String toString() {
		return "AuthTokenResponse[accessToken=***REDACTED***, refreshToken=***REDACTED***, expiresIn="
				+ expiresIn
				+ ", tokenType=" + tokenType
				+ ", usuario=" + usuario + "]";
	}
}
