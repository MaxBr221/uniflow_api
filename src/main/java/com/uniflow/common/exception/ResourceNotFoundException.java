package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends UniflowException {

	public ResourceNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso nao encontrado", message);
	}
}
