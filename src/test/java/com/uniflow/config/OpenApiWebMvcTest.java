package com.uniflow.config;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OpenApiWebMvcTest.DummyApiV1Controller.class)
@Import({OpenApiWebMvcTest.DummyApiV1Controller.class, OpenApiConfig.class})
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"springdoc.api-docs.enabled=true",
		"springdoc.swagger-ui.enabled=true"
})
class OpenApiWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldExposeOpenApiDocsWithBearerAuthAndApiV1RoutesWhenEnabled() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.info.title").value("UniFlow Backend API"))
				.andExpect(jsonPath("$.info.version").value("v1"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security[0].bearerAuth").isArray())
				.andExpect(jsonPath("$.paths['/api/v1/status']").exists());
	}

	@Test
	void shouldExposeSwaggerUiAtSwaggerUiHtmlWhenEnabled() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));

		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@RestController
	@RequestMapping("/api/v1")
	static class DummyApiV1Controller {

		@GetMapping("/status")
		Map<String, String> status() {
			return Map.of("status", "ok");
		}
	}
}
