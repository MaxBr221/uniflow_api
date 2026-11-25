package com.uniflow.auth.security;

import com.uniflow.auth.service.JwtService;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UsuarioRepository usuarioRepository;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldPopulateSecurityContextWhenBearerTokenAndActiveUsuarioAreValid() throws Exception {
		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);

		when(jwtService.extractUserId("jwt.valido")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt.valido");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.isAuthenticated()).isTrue();
		assertThat(auth.getCredentials()).isNull();
		assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

		AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
		assertThat(principal.id()).isEqualTo(userId);
		assertThat(principal.email()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(principal.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(principal.getPassword()).isNull();
		assertThat(principal.isEnabled()).isTrue();
	}

	@Test
	void shouldIgnoreRequestsWithoutValidBearerPrefix() throws Exception {
		for (String header : new String[]{null, "", "Basic abc", "Bearer", "Bearer    "}) {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
			if (header != null) {
				request.addHeader(HttpHeaders.AUTHORIZATION, header);
			}
			filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
			assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		}
		verifyNoInteractions(jwtService, usuarioRepository);
	}

	@Test
	void shouldClearSecurityContextWhenJwtIsInvalidOrExpired() throws Exception {
		when(jwtService.extractUserId("jwt.expirado"))
				.thenThrow(new ExpiredJwtException(null, null, "token expirado"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt.expirado");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(jwtService).extractUserId("jwt.expirado");
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	void shouldClearSecurityContextWhenUsuarioDoesNotExist() throws Exception {
		UUID userId = UUID.randomUUID();
		when(jwtService.extractUserId("jwt.usuario.ausente")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt.usuario.ausente");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@ParameterizedTest
	@EnumSource(value = StatusUsuario.class, names = {"INATIVO", "BLOQUEADO", "EXCLUIDO"})
	void shouldRejectAuthenticationWhenUsuarioStatusDisallowsLogin(StatusUsuario status) throws Exception {
		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);
		usuario.setStatus(status);

		when(jwtService.extractUserId("jwt.status.invalido")).thenReturn(userId);
		when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt.status.invalido");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void shouldKeepExistingAuthenticationInSecurityContextWithoutRevalidatingToken() throws Exception {
		TestingAuthenticationToken existing = new TestingAuthenticationToken("existing", null, "ROLE_USER");
		SecurityContextHolder.getContext().setAuthentication(existing);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer qualquer.token");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
		verifyNoInteractions(jwtService, usuarioRepository);
	}

	@Test
	void shouldFallbackClearPreexistingContextWhenJwtExceptionOccurs() throws Exception {
		when(jwtService.extractUserId("jwt.adulterado")).thenThrow(new JwtException("assinatura invalida"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/semestres");
		request.addHeader(HttpHeaders.AUTHORIZATION, "bearer jwt.adulterado");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
