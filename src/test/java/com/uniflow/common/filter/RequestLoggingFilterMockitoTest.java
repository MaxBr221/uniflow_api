package com.uniflow.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterMockitoTest {

	private final RequestLoggingFilter filter = new RequestLoggingFilter();

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@Test
	void shouldClearMdcEvenWhenDownstreamFilterThrowsException() throws Exception {
		when(request.getMethod()).thenReturn("POST");
		when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
		when(request.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).thenReturn(null);
		when(response.getStatus()).thenReturn(500);
		doThrow(new ServletException("erro simulado")).when(filterChain).doFilter(request, response);

		assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
				.isInstanceOf(ServletException.class)
				.hasMessage("erro simulado");

		verify(response).setHeader(
				eq(RequestLoggingFilter.REQUEST_ID_HEADER),
				matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
		);
		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
		assertThat(MDC.get(RequestLoggingFilter.HTTP_METHOD_MDC_KEY)).isNull();
		assertThat(MDC.get(RequestLoggingFilter.REQUEST_PATH_MDC_KEY)).isNull();
	}

	@Test
	void shouldPopulateMdcWithSanitizedRequestDataDuringChainExecution() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/api/v1/semestres");
		when(request.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).thenReturn("req-unit-12345");
		when(response.getStatus()).thenReturn(200);

		doAnswer(invocation -> {
			assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isEqualTo("req-unit-12345");
			assertThat(MDC.get(RequestLoggingFilter.HTTP_METHOD_MDC_KEY)).isEqualTo("GET");
			assertThat(MDC.get(RequestLoggingFilter.REQUEST_PATH_MDC_KEY)).isEqualTo("/api/v1/semestres");
			return null;
		}).when(filterChain).doFilter(request, response);

		filter.doFilterInternal(request, response, filterChain);

		verify(response).setHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "req-unit-12345");
		verify(filterChain).doFilter(request, response);
		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
	}
}
