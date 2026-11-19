package com.uniflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigPropertiesTest {

	private Properties loadYaml(String classpathLocation) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new ClassPathResource(classpathLocation));
		Properties properties = yaml.getObject();
		assertThat(properties).as("Arquivo %s deve existir e ser valido", classpathLocation).isNotNull();
		return properties;
	}

	@Test
	void baseApplicationYmlMustUseEnvVarsUtcAndValidateMode() {
		assertThat(Path.of("src/main/resources/application.properties")).doesNotExist();

		Properties base = loadYaml("application.yml");
		assertThat(base.getProperty("spring.application.name")).isEqualTo("uniflow-backend");
		assertThat(base.getProperty("spring.profiles.active")).isEqualTo("${SPRING_PROFILES_ACTIVE:dev}");
		assertThat(base.getProperty("spring.datasource.url"))
				.startsWith("${DB_URL:")
				.contains("jdbc:postgresql://");
		assertThat(base.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
		assertThat(base.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
		assertThat(base.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
		assertThat(base.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
		assertThat(base.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(base.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone")).isEqualTo("UTC");
		assertThat(base.getProperty("spring.jackson.time-zone")).isEqualTo("UTC");
		assertThat(base.getProperty("app.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
		assertThat(base.getProperty("app.cors.allowed-origins")).startsWith("${CORS_ALLOWED_ORIGINS:");
	}

	@Test
	void environmentProfilesMustFollowDevTestAndProdPolicies() {
		Properties dev = loadYaml("application-dev.yml");
		assertThat(dev.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
		assertThat(dev.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");

		Properties test = loadYaml("application-test.yml");
		assertThat(test.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
		assertThat(test.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(test.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone")).isEqualTo("UTC");
		assertThat(test.getProperty("spring.jackson.time-zone")).isEqualTo("UTC");
		assertThat(test.getProperty("app.security.jwt.secret")).isNotBlank();

		Properties prod = loadYaml("application-prod.yml");
		assertThat(prod.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
		assertThat(prod.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
		assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
		assertThat(prod.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
		assertThat(prod.getProperty("spring.jpa.show-sql")).isEqualTo("false");
		assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(prod.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone")).isEqualTo("UTC");
		assertThat(prod.getProperty("server.error.include-message")).isEqualTo("never");
		assertThat(prod.getProperty("server.error.include-stacktrace")).isEqualTo("never");
		assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("${SWAGGER_ENABLED:false}");
		assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo("${API_DOCS_ENABLED:false}");
		assertThat(prod.getProperty("app.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
		assertThat(prod.getProperty("app.cors.allowed-origins")).isEqualTo("${CORS_ALLOWED_ORIGINS}");
	}

	@Test
	void composeEnvExampleAndGitignoreMustBeConfiguredWithoutHardcodedSecrets() throws IOException {
		Path composePath = Path.of("compose.yaml");
		Path envExamplePath = Path.of(".env.example");
		Path gitignorePath = Path.of(".gitignore");

		assertThat(composePath).exists();
		assertThat(envExamplePath).exists();
		assertThat(gitignorePath).exists();

		String composeContent = Files.readString(composePath);
		assertThat(composeContent)
				.contains("postgres:16-alpine")
				.contains("POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?")
				.contains("pg_isready");

		String envExampleContent = Files.readString(envExamplePath);
		assertThat(envExampleContent)
				.contains("POSTGRES_DB=")
				.contains("POSTGRES_USER=")
				.contains("POSTGRES_PASSWORD=")
				.contains("DB_URL=")
				.contains("DB_USERNAME=")
				.contains("DB_PASSWORD=")
				.contains("JWT_SECRET=");

		String gitignoreContent = Files.readString(gitignorePath);
		assertThat(gitignoreContent)
				.contains(".env")
				.contains("!.env.example");
	}
}
