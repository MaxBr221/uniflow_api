package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends UniflowException {

	public UnauthorizedOperationException(String message) {
		super(HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", "Operacao nao autorizada", message);
	}
}
