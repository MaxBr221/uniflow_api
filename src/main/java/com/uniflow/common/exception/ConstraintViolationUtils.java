package com.uniflow.common.exception;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

/**
 * Utilitario para inspecao de violacoes de constraints nomeadas em cadeias de excecoes de persistencia.
 */
public final class ConstraintViolationUtils {

	private ConstraintViolationUtils() {
	}

	public static boolean containsConstraint(Throwable throwable, String constraintName) {
		if (throwable == null || constraintName == null || constraintName.isBlank()) {
			return false;
		}
		String expectedLower = constraintName.toLowerCase(Locale.ROOT);
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ConstraintViolationException cve
					&& cve.getConstraintName() != null
					&& cve.getConstraintName().toLowerCase(Locale.ROOT).contains(expectedLower)) {
				return true;
			}
			String message = current.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains(expectedLower)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
