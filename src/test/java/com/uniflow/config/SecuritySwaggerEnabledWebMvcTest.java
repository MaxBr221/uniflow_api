package com.uniflow.config;

import com.uniflow.auth.service.JwtService;
import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecuritySwaggerEnabledWebMvcTest.DummyRouteController.class)
@Import({
		SecuritySwaggerEnabledWebMvcTest.DummyRouteController.class,
		SecurityConfig.class,
		OpenApiConfig.class,
		GlobalExceptionHandler.class
})
@ImportAutoConfiguration({
		SpringDocConfiguration.class,
		SpringDocConfigProperties.class,
		SpringDocSpecPropertiesConfiguration.class,
		SpringDocWebMvcConfiguration.class,
		MultipleOpenApiSupportConfiguration.class,
		SwaggerConfig.class,
		SwaggerUiConfigProperties.class,
		SwaggerUiOAuthProperties.class
})
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"springdoc.api-docs.enabled=true",
		"springdoc.swagger-ui.enabled=true"
})
class SecuritySwaggerEnabledWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void shouldAllowPublicOpenApiAndSwaggerWhenEnabledWhileKeepingApiRoutesAuthenticated() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isFound());
		mockMvc.perform(get("/api/v1/protected-sample")).andExpect(status().isUnauthorized());
	}

	@RestController
	@RequestMapping("/api/v1")
	static class DummyRouteController {

		@GetMapping("/protected-sample")
		Map<String, Object> protectedSample() {
			return Map.of("ok", true);
		}
	}
}
