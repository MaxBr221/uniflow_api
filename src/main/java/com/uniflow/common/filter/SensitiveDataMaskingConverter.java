package com.uniflow.common.filter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Conversor Logback customizado para mascarar tokens JWT, senhas e credenciais em mensagens de log.
 */
public class SensitiveDataMaskingConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent event) {
		if (event == null) {
			return "";
		}
		String formattedMessage = event.getFormattedMessage();
		return formattedMessage == null ? "" : SensitiveDataMasker.mask(formattedMessage);
	}
}
