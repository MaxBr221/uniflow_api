package com.uniflow.common.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestLoggingFilterWebMvcTest.DummyPingController.class)
@Import({RequestLoggingFilterWebMvcTest.DummyPingController.class, RequestLoggingFilter.class})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class RequestLoggingFilterWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldGenerateRequestIdPopulateMdcLogCompletionWithoutSecretsAndClearMdc() throws Exception {
		Logger filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		filterLogger.addAppender(appender);

		try {
			mockMvc.perform(get("/test-logging/ping")
							.with(user("tester"))
							.queryParam("token", "query-secret-nao-logar")
							.header("Authorization", "Bearer header-jwt-super-secreto"))
					.andExpect(status().isOk())
					.andExpect(header().string(
							RequestLoggingFilter.REQUEST_ID_HEADER,
							matchesRegex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
					))
					.andExpect(jsonPath("$.requestId", matchesRegex("^[0-9a-f-]{36}$")));

			assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
			assertThat(MDC.get(RequestLoggingFilter.HTTP_METHOD_MDC_KEY)).isNull();
			assertThat(MDC.get(RequestLoggingFilter.REQUEST_PATH_MDC_KEY)).isNull();

			assertThat(appender.list).isNotEmpty();
			ILoggingEvent logEvent = appender.list.getLast();
			assertThat(logEvent.getMDCPropertyMap()).containsKey(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
			assertThat(logEvent.getFormattedMessage())
					.contains("method=GET")
					.contains("path=/test-logging/ping")
					.contains("status=200")
					.doesNotContain("query-secret-nao-logar")
					.doesNotContain("header-jwt-super-secreto");
		} finally {
			filterLogger.detachAppender(appender);
			appender.stop();
		}
	}

	@Test
	void shouldReuseValidIncomingRequestIdRejectUnsafeAndRunBeforeSecurityFilter() throws Exception {
		mockMvc.perform(get("/test-logging/ping")
						.with(user("tester"))
						.header(RequestLoggingFilter.REQUEST_ID_HEADER, "front-req-20261119"))
				.andExpect(status().isOk())
				.andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, "front-req-20261119"))
				.andExpect(jsonPath("$.requestId").value("front-req-20261119"));

		mockMvc.perform(get("/test-logging/ping")
						.with(user("tester"))
						.header(RequestLoggingFilter.REQUEST_ID_HEADER, "invalid$req id!"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						RequestLoggingFilter.REQUEST_ID_HEADER,
						matchesRegex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
				));

		mockMvc.perform(get("/test-logging/ping"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(
						RequestLoggingFilter.REQUEST_ID_HEADER,
						matchesRegex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
				));
	}

	@RestController
	@RequestMapping("/test-logging")
	static class DummyPingController {

		@GetMapping("/ping")
		Map<String, String> ping() {
			return Map.of("requestId", String.valueOf(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)));
		}
	}
}
