package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends UniflowException {

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "Conflito de recurso", message);
	}
}
