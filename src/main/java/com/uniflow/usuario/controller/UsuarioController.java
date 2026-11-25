package com.uniflow.usuario.controller;

import com.uniflow.usuario.dto.UpdateProfileRequest;
import com.uniflow.usuario.dto.UsuarioResponse;
import com.uniflow.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST para consulta e gerenciamento do perfil do usuario autenticado ({@code /api/v1/usuarios}).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Consulta e atualizacao do perfil do usuario autenticado")
public class UsuarioController {

	private final UsuarioService usuarioService;

	/**
	 * Retorna o perfil do usuario atualmente autenticado no {@code SecurityContext}.
	 *
	 * @return dados publicos do perfil ({@code 200 OK})
	 */
	@GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Consultar perfil do usuario autenticado",
			description = "Retorna os dados publicos da conta autenticada lendo a identidade exclusivamente do SecurityContext."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Perfil consultado com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsuarioResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Nao autenticado ou token JWT invalido/expirado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<UsuarioResponse> getProfile() {
		return ResponseEntity.ok(usuarioService.getProfile());
	}

	/**
	 * Atualiza parcialmente os dados pessoais permitidos ({@code nome} e/ou {@code email})
	 * do usuario autenticado no {@code SecurityContext}.
	 *
	 * @param request payload validado com os novos dados pessoais permitidos
	 * @return perfil atualizado sem exposicao de campos sensiveis ({@code 200 OK})
	 */
	@PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Atualizar perfil do usuario autenticado",
			description = "Atualiza dados pessoais permitidos (nome e/ou e-mail) do usuario autenticado, validando unicidade de e-mail e ignorando identificadores externos."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Perfil atualizado com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsuarioResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Nao autenticado ou token JWT invalido/expirado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "409",
					description = "E-mail ja utilizado por outra conta",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<UsuarioResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(usuarioService.updateProfile(request));
	}
}
