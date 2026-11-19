package com.uniflow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class UniflowException extends RuntimeException {

	private final HttpStatus status;
	private final String errorCode;
	private final String title;

	protected UniflowException(HttpStatus status, String errorCode, String title, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
		this.title = title;
	}

	protected UniflowException(HttpStatus status, String errorCode, String title, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.errorCode = errorCode;
		this.title = title;
	}
}
