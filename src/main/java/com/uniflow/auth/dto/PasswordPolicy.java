package com.uniflow.auth.dto;

/**
 * Constantes compartilhadas da politica de complexidade e tamanho de senhas ({@code MOD-01}).
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 72;
	public static final String STRONG_PASSWORD_REGEX =
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$";

	private PasswordPolicy() {
	}
}
