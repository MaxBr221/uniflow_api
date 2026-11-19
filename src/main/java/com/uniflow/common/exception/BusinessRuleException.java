package com.uniflow.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends UniflowException {

	public BusinessRuleException(String message) {
		super(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION", "Regra de negocio violada", message);
	}
}
