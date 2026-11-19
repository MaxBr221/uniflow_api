package com.uniflow.common.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitario para mascaramento de credenciais, tokens e segredos antes da emissao em logs,
 * alem de sanitizacao contra log injection (CRLF).
 */
public final class SensitiveDataMasker {

	private static final String REDACTED = "***REDACTED***";
	private static final String REDACTED_JWT = "***REDACTED_JWT***";

	private static final Pattern AUTH_SCHEME_PATTERN = Pattern.compile(
			"(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9\\-._~+/]+=*"
	);

	private static final Pattern JWT_PATTERN = Pattern.compile(
			"\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b"
	);

	private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
			"(?i)(\"(?:password|senha|senhaAtual|novaSenha|senhaHash|passwordHash|secret|jwtSecret|token|"
					+ "accessToken|refreshToken|access_token|refresh_token|authorization|apiKey|api_key|credential|credentials)\""
					+ "|\\b(?:password|senha|senhaAtual|novaSenha|senhaHash|passwordHash|secret|jwtSecret|token|"
					+ "accessToken|refreshToken|access_token|refresh_token|authorization|apiKey|api_key|credential|credentials)\\b)"
					+ "\\s*([:=])\\s*"
					+ "(?:\"([^\"\\r\\n]*)\"|'([^'\\r\\n]*)'|(?:(?:Bearer|Basic)\\s+)?([^\\s,;}\\]]+))"
	);

	private SensitiveDataMasker() {
	}

	public static String mask(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		String masked = maskKeyValues(input);
		masked = AUTH_SCHEME_PATTERN.matcher(masked).replaceAll("$1 " + REDACTED);
		masked = JWT_PATTERN.matcher(masked).replaceAll(REDACTED_JWT);
		return masked;
	}

	public static String sanitizeLogValue(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll("[\\r\\n\\t]+", "_");
	}

	private static String maskKeyValues(String input) {
		Matcher matcher = SENSITIVE_KEY_VALUE_PATTERN.matcher(input);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String keyToken = matcher.group(1);
			String separator = matcher.group(2);
			String replacement;
			if (matcher.group(3) != null) {
				replacement = keyToken + separator + " \"" + REDACTED + "\"";
				if (separator.equals("=")) {
					replacement = keyToken + "=\"" + REDACTED + "\"";
				}
			} else if (matcher.group(4) != null) {
				replacement = keyToken + separator + "'" + REDACTED + "'";
			} else {
				replacement = keyToken + separator + (":".equals(separator) ? " " : "") + REDACTED;
			}
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
