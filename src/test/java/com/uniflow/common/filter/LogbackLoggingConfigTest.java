package com.uniflow.common.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackLoggingConfigTest {

	@Test
	void logbackSpringXmlMustDeclareMaskingConverterUtcTimestampsAndRequestIdMdc() throws IOException {
		ClassPathResource resource = new ClassPathResource("logback-spring.xml");
		assertThat(resource.exists()).isTrue();

		String xml = resource.getContentAsString(StandardCharsets.UTF_8);
		assertThat(xml)
				.contains("com.uniflow.common.filter.SensitiveDataMaskingConverter")
				.contains("conversionWord=\"maskedMsg\"")
				.contains("%X{requestId:-none}")
				.contains("%maskedMsg")
				.contains("UTC")
				.contains("<springProfile name=\"!prod\">")
				.contains("<springProfile name=\"prod\">");
	}

	@Test
	void maskingConverterAndPatternLayoutMustIncludeRequestIdAndRedactSecrets() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = context.getLogger("com.uniflow.test.Audit");

		PatternLayout layout = new PatternLayout();
		layout.setContext(context);
		layout.getInstanceConverterMap().put("maskedMsg", SensitiveDataMaskingConverter::new);
		layout.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z', UTC} %-5level [requestId=%X{requestId:-none}] - %maskedMsg");
		layout.start();

		MDC.put(RequestLoggingFilter.REQUEST_ID_MDC_KEY, "req-1234-5678");
		try {
			ILoggingEvent event = new LoggingEvent(
					Logger.FQCN,
					logger,
					Level.INFO,
					"Login tentativa password={} token={}",
					null,
					new Object[]{"SenhaSecreta123!", "Bearer jwt.secreto.123"}
			);

			String formatted = layout.doLayout(event);
			assertThat(formatted)
					.contains("[requestId=req-1234-5678]")
					.contains("password=***REDACTED***")
					.doesNotContain("SenhaSecreta123!")
					.doesNotContain("jwt.secreto.123");
		} finally {
			MDC.clear();
			layout.stop();
		}
	}
}
