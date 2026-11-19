package com.uniflow.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
	private static final String VALIDATION_ERROR_TITLE = "Dados invalidos";
	private static final String VALIDATION_ERROR_DETAIL = "Um ou mais campos estao invalidos.";
	private static final Comparator<FieldErrorDetail> FIELD_ERROR_COMPARATOR = Comparator
			.comparing(FieldErrorDetail::field, Comparator.nullsLast(String::compareTo))
			.thenComparing(FieldErrorDetail::message, Comparator.nullsLast(String::compareTo));

	@ExceptionHandler(UniflowException.class)
	public ResponseEntity<ProblemDetail> handleUniflowException(UniflowException ex, HttpServletRequest request) {
		log.warn("Domain exception [{}] on {}: {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
		return buildResponse(ex.getStatus(), ex.getErrorCode(), ex.getTitle(), ex.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> handleAuthenticationException(
			AuthenticationException ex,
			HttpServletRequest request) {
		log.warn("Authentication failure on {}: {}", request.getRequestURI(), ex.getMessage());
		return buildResponse(
				HttpStatus.UNAUTHORIZED,
				"AUTHENTICATION_FAILED",
				"Nao autenticado",
				"Autenticacao necessaria ou credenciais invalidas.",
				request
		);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDeniedException(
			AccessDeniedException ex,
			HttpServletRequest request) {
		log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
		return buildResponse(
				HttpStatus.FORBIDDEN,
				"FORBIDDEN_OPERATION",
				"Acesso negado",
				"Voce nao possui permissao para executar esta operacao.",
				request
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorDetail> fieldErrors = new ArrayList<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.add(new FieldErrorDetail(error.getField(), resolveValidationMessage(error.getDefaultMessage())));
		}
		for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
			fieldErrors.add(new FieldErrorDetail(error.getObjectName(), resolveValidationMessage(error.getDefaultMessage())));
		}
		log.debug("Bean Validation failure on {} ({} field error(s))", request.getRequestURI(), fieldErrors.size());
		return buildValidationResponse(fieldErrors, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException ex,
			HttpServletRequest request) {
		List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
				.map(this::toFieldErrorDetail)
				.toList();
		log.debug("Constraint violation on {} ({} field error(s))", request.getRequestURI(), fieldErrors.size());
		return buildValidationResponse(fieldErrors, request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ProblemDetail> handleHandlerMethodValidation(
			HandlerMethodValidationException ex,
			HttpServletRequest request) {
		List<FieldErrorDetail> fieldErrors = new ArrayList<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			String parameterName = result.getMethodParameter().getParameterName();
			String fallbackField = (parameterName != null && !parameterName.isBlank()) ? parameterName : "parametro";
			for (MessageSourceResolvable error : result.getResolvableErrors()) {
				fieldErrors.add(toFieldErrorDetail(error, fallbackField));
			}
		}
		for (MessageSourceResolvable error : ex.getCrossParameterValidationResults()) {
			fieldErrors.add(toFieldErrorDetail(error, "request"));
		}
		log.debug("Method parameter validation failure on {} ({} field error(s))", request.getRequestURI(), fieldErrors.size());
		return buildValidationResponse(fieldErrors, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		log.debug("Malformed request body on {}: {}", request.getRequestURI(), ex.getMessage());
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST_BODY",
				"Corpo da requisicao invalido",
				"Nao foi possivel ler ou interpretar o JSON enviado.",
				request
		);
	}

	@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ProblemDetail> handleBadRequestParameters(
			Exception ex,
			HttpServletRequest request) {
		String detail = ex instanceof MissingServletRequestParameterException missing
				? "Parametro obrigatorio ausente: " + missing.getParameterName()
				: "Parametro de requisicao com formato invalido.";
		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_PARAMETER", "Parametro invalido", detail, request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNoResourceFound(
			NoResourceFoundException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.NOT_FOUND,
				"ROUTE_NOT_FOUND",
				"Rota nao encontrada",
				"O recurso solicitado nao foi encontrado.",
				request
		);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED",
				"Metodo HTTP nao suportado",
				"Metodo " + ex.getMethod() + " nao e suportado para esta rota.",
				request
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {}", request.getRequestURI(), ex);
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				ProblemDetailsFactory.DEFAULT_ERROR_CODE,
				"Erro interno do servidor",
				"Ocorreu um erro inesperado ao processar a requisicao.",
				request
		);
	}

	private ResponseEntity<ProblemDetail> buildValidationResponse(
			List<FieldErrorDetail> fieldErrors,
			HttpServletRequest request) {
		List<FieldErrorDetail> sortedErrors = fieldErrors.stream()
				.sorted(FIELD_ERROR_COMPARATOR)
				.toList();
		return ProblemDetailsFactory.createResponse(
				HttpStatus.BAD_REQUEST,
				VALIDATION_ERROR_CODE,
				VALIDATION_ERROR_TITLE,
				VALIDATION_ERROR_DETAIL,
				request.getRequestURI(),
				sortedErrors
		);
	}

	private FieldErrorDetail toFieldErrorDetail(ConstraintViolation<?> violation) {
		return new FieldErrorDetail(
				extractFieldName(violation.getPropertyPath()),
				resolveValidationMessage(violation.getMessage())
		);
	}

	private FieldErrorDetail toFieldErrorDetail(MessageSourceResolvable error, String fallbackField) {
		String field = switch (error) {
			case FieldError fieldError -> fieldError.getField();
			case ObjectError objectError -> objectError.getObjectName();
			default -> fallbackField;
		};
		return new FieldErrorDetail(field, resolveValidationMessage(error.getDefaultMessage()));
	}

	private String extractFieldName(Path propertyPath) {
		if (propertyPath == null) {
			return "campo";
		}
		List<Path.Node> meaningfulNodes = new ArrayList<>();
		for (Path.Node node : propertyPath) {
			ElementKind kind = node.getKind();
			if (kind != ElementKind.METHOD && kind != ElementKind.CONSTRUCTOR) {
				meaningfulNodes.add(node);
			}
		}
		if (meaningfulNodes.size() > 1 && meaningfulNodes.getFirst().getKind() == ElementKind.PARAMETER) {
			meaningfulNodes.removeFirst();
		}
		List<String> segments = new ArrayList<>();
		for (Path.Node node : meaningfulNodes) {
			String text = node.toString();
			if (text != null && !text.isBlank()) {
				segments.add(text);
			}
		}
		return segments.isEmpty() ? propertyPath.toString() : String.join(".", segments);
	}

	private String resolveValidationMessage(String message) {
		return (message != null && !message.isBlank()) ? message : "valor invalido";
	}

	private ResponseEntity<ProblemDetail> buildResponse(
			HttpStatus status,
			String code,
			String title,
			String detail,
			HttpServletRequest request) {
		return ProblemDetailsFactory.createResponse(
				status,
				code,
				title,
				detail,
				request.getRequestURI()
		);
	}
}
