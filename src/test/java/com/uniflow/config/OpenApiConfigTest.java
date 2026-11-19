package com.uniflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

	@Test
	void shouldConfigureOpenApiMetadataServerAndJwtBearerSecurityScheme() {
		OpenApiConfig config = new OpenApiConfig();

		OpenAPI openAPI = config.uniflowOpenAPI();

		assertThat(openAPI.getInfo()).isNotNull();
		assertThat(openAPI.getInfo().getTitle()).isEqualTo("UniFlow Backend API");
		assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
		assertThat(openAPI.getInfo().getDescription()).contains("/api/v1");

		assertThat(openAPI.getServers())
				.isNotEmpty()
				.anySatisfy(server -> {
					assertThat(server.getUrl()).isEqualTo("/");
					assertThat(server.getDescription()).contains("/api/v1");
				});

		assertThat(openAPI.getComponents()).isNotNull();
		SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH_SCHEME);
		assertThat(scheme).isNotNull();
		assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
		assertThat(scheme.getScheme()).isEqualTo("bearer");
		assertThat(scheme.getBearerFormat()).isEqualTo("JWT");

		assertThat(openAPI.getSecurity())
				.anySatisfy(req -> assertThat(req).containsKey(OpenApiConfig.BEARER_AUTH_SCHEME));
	}
}
