package com.uniflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuracao central da especificacao OpenAPI 3 / Swagger UI da API REST `/api/v1`.
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI uniflowOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("UniFlow Backend API")
						.version("v1")
						.description("API REST do backend da plataforma UniFlow para organizacao da rotina academica (/api/v1).")
						.contact(new Contact().name("UniFlow Backend")))
				.servers(List.of(new Server()
						.url("/")
						.description("Servidor atual (endpoints versionados sob /api/v1)")))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
								.name(BEARER_AUTH_SCHEME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Informe o access token JWT obtido em /api/v1/auth/login.")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
	}
}
