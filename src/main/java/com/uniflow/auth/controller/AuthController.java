package com.uniflow.auth.controller;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.ChangePasswordRequest;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.LogoutRequest;
import com.uniflow.auth.dto.RefreshTokenRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.security.AuthenticatedUser;
import com.uniflow.auth.service.AuthService;
import com.uniflow.common.exception.AuthenticationFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST do modulo de autenticacao ({@code /api/v1/auth}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Operacoes de cadastro e autenticacao de estudantes")
public class AuthController {

	private final AuthService authService;

	/**
	 * Cadastra uma nova conta de estudante na plataforma.
	 *
	 * @param request dados validados do cadastro
	 * @return usuario recem-criado sem campos sensiveis ({@code 201 Created})
	 */
	@PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirements
	@Operation(
			summary = "Cadastrar estudante",
			description = "Cria uma nova conta com e-mail normalizado em lowercase, validacao de senha forte e hash BCrypt."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "Usuario cadastrado com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthUserResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "409",
					description = "E-mail ja cadastrado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthUserResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Autentica um estudante cadastrado e emite access token JWT e refresh token opaco.
	 *
	 * @param request credenciais de acesso validadas
	 * @return resposta com {@code accessToken}, {@code refreshToken}, metadados de expiracao e dados do usuario ({@code 200 OK})
	 */
	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirements
	@Operation(
			summary = "Autenticar estudante",
			description = "Valida credenciais via BCrypt e emite access token JWT assinado (claims sub e jti) e refresh token opaco para rotacao."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Autenticacao realizada com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthTokenResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Credenciais invalidas",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * Renova a sessao rotacionando o refresh token ativo e emitindo novo access token JWT.
	 *
	 * @param request payload validado contendo o refresh token atual
	 * @return resposta com novos {@code accessToken}, {@code refreshToken} rotacionado e dados do usuario ({@code 200 OK})
	 */
	@PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirements
	@Operation(
			summary = "Renovar sessao",
			description = "Valida e rotaciona o refresh token ativo, emitindo novo access token JWT e bloqueando reutilizacao de tokens ja rotacionados/revogados."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Sessao renovada com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthTokenResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Refresh token invalido, expirado ou reutilizado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	/**
	 * Encerra a sessao do estudante autenticado revogando o refresh token ativo
	 * (ou todas as sessoes ativas quando omitido) e limpando o {@code SecurityContext}.
	 *
	 * @param authenticatedUser usuario autenticado extraido do {@code SecurityContext}
	 * @param request           payload opcional contendo o refresh token a revogar
	 * @return {@code 204 No Content}
	 */
	@PostMapping("/logout")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(
			summary = "Encerrar sessao (logout)",
			description = "Revoga o refresh token informado (validando propriedade do usuario autenticado) ou todos os refresh tokens ativos da conta quando omitido."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Sessao encerrada com sucesso"),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Nao autenticado ou access token JWT invalido/expirado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "Tentativa de revogar refresh token pertencente a outro usuario",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<Void> logout(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody(required = false) LogoutRequest request) {
		if (authenticatedUser == null) {
			throw new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas.");
		}
		authService.logout(authenticatedUser.id(), request);
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

	/**
	 * Retorna os dados publicos da conta autenticada lendo a identidade exclusivamente do {@code SecurityContext}.
	 *
	 * @return dados publicos do usuario autenticado sem {@code senhaHash} ({@code 200 OK})
	 */
	@GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(
			summary = "Consultar usuario autenticado",
			description = "Retorna os dados publicos do usuario autenticado exclusivamente via SecurityContext, sem confiar em usuarioId enviado pelo cliente."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Dados do usuario autenticado retornados com sucesso",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthUserResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Nao autenticado ou token JWT invalido/expirado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<AuthUserResponse> me() {
		return ResponseEntity.ok(authService.getCurrentUser());
	}

	/**
	 * Altera a senha do usuario autenticado mediante validacao da senha atual e revoga sessoes de refresh token.
	 *
	 * @param request payload validado com {@code senhaAtual} e {@code novaSenha}
	 * @return {@code 204 No Content}
	 */
	@PatchMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(
			summary = "Alterar senha do usuario autenticado",
			description = "Valida a senha atual via BCrypt, persiste a nova senha forte com novo hash BCrypt e revoga todos os refresh tokens ativos da conta."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
			@ApiResponse(
					responseCode = "400",
					description = "Dados de entrada invalidos",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "Nao autenticado ou access token JWT invalido/expirado",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(
					responseCode = "422",
					description = "Senha atual incorreta ou nova senha igual a senha atual",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
			)
	})
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ResponseEntity.noContent().build();
	}
}
