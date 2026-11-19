package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends UniflowException {

	public AuthenticationFailedException(String message) {
		super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Falha de autenticacao", message);
	}
}
