package com.uniflow.config;

import com.uniflow.auth.security.JwtAuthenticationFilter;
import com.uniflow.auth.service.JwtService;
import com.uniflow.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

/**
 * Configuracao central do Spring Security em modo stateless ({@code RNF-002}, {@code RNF-003}).
 * <p>
 * Libera exclusivamente os endpoints publicos de autenticacao e, quando habilitados por perfil,
 * os endpoints de documentacao OpenAPI/Swagger, exigindo autenticacao nas demais rotas e
 * delegando respostas {@code 401} e {@code 403} ao {@link HandlerExceptionResolver} para padronizacao RFC 7807.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	public static final String[] PUBLIC_AUTH_POST_ENDPOINTS = {
			"/api/v1/auth/register",
			"/api/v1/auth/login",
			"/api/v1/auth/refresh"
	};

	public static final String[] OPENAPI_DOC_ENDPOINTS = {
			"/v3/api-docs",
			"/v3/api-docs/**"
	};

	public static final String[] SWAGGER_UI_ENDPOINTS = {
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler,
			ObjectProvider<JwtService> jwtServiceProvider,
			ObjectProvider<UsuarioRepository> usuarioRepositoryProvider,
			@Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled,
			@Value("${springdoc.swagger-ui.enabled:false}") boolean swaggerUiEnabled) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(RequestCacheConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> {
					authorize.requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POST_ENDPOINTS).permitAll();
					if (apiDocsEnabled) {
						authorize.requestMatchers(HttpMethod.GET, OPENAPI_DOC_ENDPOINTS).permitAll();
					}
					if (swaggerUiEnabled) {
						authorize.requestMatchers(HttpMethod.GET, SWAGGER_UI_ENDPOINTS).permitAll();
					}
					authorize.anyRequest().authenticated();
				});

		JwtService jwtService = jwtServiceProvider.getIfAvailable();
		UsuarioRepository usuarioRepository = usuarioRepositoryProvider.getIfAvailable();
		if (jwtService != null && usuarioRepository != null) {
			http.addFilterBefore(
					new JwtAuthenticationFilter(jwtService, usuarioRepository),
					UsernamePasswordAuthenticationFilter.class
			);
		}

		return http.build();
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
		return (request, response, authException) -> {
			if (handlerExceptionResolver.resolveException(request, response, null, authException) == null
					&& !response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Nao autenticado");
			}
		};
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
		return (request, response, accessDeniedException) -> {
			if (handlerExceptionResolver.resolveException(request, response, null, accessDeniedException) == null
					&& !response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
			}
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
		List<String> cleanOrigins = allowedOrigins.stream()
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(cleanOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "Accept", "Origin"));
		configuration.setExposedHeaders(List.of("X-Request-Id", "Location"));
		configuration.setAllowCredentials(!cleanOrigins.isEmpty() && cleanOrigins.stream().noneMatch("*"::equals));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
