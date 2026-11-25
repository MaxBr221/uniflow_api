package com.uniflow.auth.controller;

import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
	 * Autentica um estudante cadastrado e emite access token JWT.
	 *
	 * @param request credenciais de acesso validadas
	 * @return resposta com {@code accessToken}, metadados de expiracao e dados do usuario ({@code 200 OK})
	 */
	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirements
	@Operation(
			summary = "Autenticar estudante",
			description = "Valida credenciais via BCrypt e emite access token JWT assinado com claims sub e jti."
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
}
