package com.uniflow.auth.dto;

import com.uniflow.usuario.enums.StatusUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Representacao publica segura do usuario cadastrado ou autenticado, sem exposicao de {@code senhaHash}.
 */
@Schema(description = "Dados publicos do usuario autenticado ou recem-cadastrado")
public record AuthUserResponse(
		@Schema(description = "Identificador unico do usuario", example = "550e8400-e29b-41d4-a716-446655440000")
		UUID id,

		@Schema(description = "Nome completo do usuario", example = "Ana Silva")
		String nome,

		@Schema(description = "E-mail normalizado do usuario", example = "ana.silva@uniflow.edu")
		String email,

		@Schema(description = "Status atual da conta do usuario", example = "ATIVO")
		StatusUsuario status,

		@Schema(description = "Instante de criacao da conta em UTC", example = "2026-11-22T15:30:00Z")
		Instant createdAt,

		@Schema(description = "Instante da ultima atualizacao da conta em UTC", example = "2026-11-22T15:30:00Z")
		Instant updatedAt
) {
}
