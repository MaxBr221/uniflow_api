package com.uniflow.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtro de correlacao HTTP que popula {@code requestId}, metodo e caminho no {@link MDC}
 * sem registrar headers sensiveis, query strings ou payloads de requisicao.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String HTTP_METHOD_MDC_KEY = "httpMethod";
	public static final String REQUEST_PATH_MDC_KEY = "requestPath";

	private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		long startNanos = System.nanoTime();
		String requestId = resolveRequestId(request);
		String method = SensitiveDataMasker.sanitizeLogValue(request.getMethod());
		String path = SensitiveDataMasker.sanitizeLogValue(request.getRequestURI());

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		MDC.put(HTTP_METHOD_MDC_KEY, method);
		MDC.put(REQUEST_PATH_MDC_KEY, path);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			int status = response.getStatus();
			if (isDocumentationAsset(path)) {
				log.debug("HTTP request completed method={} path={} status={} durationMs={}", method, path, status, durationMs);
			} else {
				log.info("HTTP request completed method={} path={} status={} durationMs={}", method, path, status, durationMs);
			}
			MDC.remove(REQUEST_ID_MDC_KEY);
			MDC.remove(HTTP_METHOD_MDC_KEY);
			MDC.remove(REQUEST_PATH_MDC_KEY);
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		String incoming = request.getHeader(REQUEST_ID_HEADER);
		if (incoming != null && SAFE_REQUEST_ID_PATTERN.matcher(incoming.trim()).matches()) {
			return incoming.trim();
		}
		return UUID.randomUUID().toString();
	}

	private boolean isDocumentationAsset(String path) {
		return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || "/favicon.ico".equals(path);
	}
}
