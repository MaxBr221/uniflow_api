package com.uniflow.auth.security;

import com.uniflow.auth.service.JwtService;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Filtro stateless que valida o access token JWT enviado no cabecalho {@code Authorization},
 * verifica o status ativo do usuario no banco e popula o {@link SecurityContext}.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UsuarioRepository usuarioRepository;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = extractBearerToken(request);
		if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			UUID userId = jwtService.extractUserId(token);
			Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
			if (usuarioOpt.isEmpty() || !usuarioOpt.get().podeAutenticar()) {
				log.debug("Autenticacao JWT recusada: usuario inexistente ou inapto (id={})", userId);
				SecurityContextHolder.clearContext();
				filterChain.doFilter(request, response);
				return;
			}

			AuthenticatedUser principal = AuthenticatedUser.fromUsuario(usuarioOpt.get());
			UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
					principal,
					null,
					principal.getAuthorities()
			);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
		} catch (JwtException | IllegalArgumentException ex) {
			log.debug("Token JWT invalido ou expirado: {}", ex.getClass().getSimpleName());
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private static String extractBearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || header.length() <= BEARER_PREFIX.length()) {
			return null;
		}
		if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}
		String token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}
}
