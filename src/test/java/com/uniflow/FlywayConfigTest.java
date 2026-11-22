package com.uniflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayConfigTest {

	private static final Pattern MIGRATION_NAME_PATTERN = Pattern.compile("^V(\\d+)__([a-z0-9_]+)\\.sql$");

	@Test
	void flywayPropertiesMustBeConfiguredSafelyInApplicationYml() {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new ClassPathResource("application.yml"));
		Properties base = yaml.getObject();

		assertThat(base).isNotNull();
		assertThat(base.getProperty("spring.flyway.enabled")).isEqualTo("true");
		assertThat(base.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
		assertThat(base.getProperty("spring.flyway.encoding")).isEqualTo("UTF-8");
		assertThat(base.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
		assertThat(base.getProperty("spring.flyway.validate-on-migrate")).isEqualTo("true");
		assertThat(base.getProperty("spring.flyway.validate-migration-naming")).isEqualTo("true");
		assertThat(base.getProperty("spring.flyway.fail-on-missing-locations")).isEqualTo("true");
		assertThat(base.getProperty("spring.flyway.out-of-order")).isEqualTo("false");
		assertThat(base.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
		assertThat(base.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
	}

	@Test
	void baselineMigrationMustExistAndFollowSequentialNamingConvention() throws IOException {
		Path migrationDir = Path.of("src/main/resources/db/migration");
		assertThat(migrationDir).exists().isDirectory();

		List<Path> files;
		try (var stream = Files.list(migrationDir)) {
			files = stream.filter(Files::isRegularFile).sorted().toList();
		}

		assertThat(files).isNotEmpty();

		Set<String> seenVersions = new HashSet<>();
		for (Path file : files) {
			String fileName = file.getFileName().toString();
			Matcher matcher = MIGRATION_NAME_PATTERN.matcher(fileName);
			assertThat(matcher.matches())
					.as("Migration '%s' deve seguir padrao V<numero>__<snake_case>.sql", fileName)
					.isTrue();

			String version = matcher.group(1);
			assertThat(seenVersions.add(version))
					.as("Versao duplicada de migration detectada: V%s", version)
					.isTrue();
		}

		Path v1 = migrationDir.resolve("V1__init_baseline.sql");
		assertThat(v1).exists();
		assertThat(Files.readString(v1)).contains("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
		assertThat(new ClassPathResource("db/migration/V1__init_baseline.sql").exists()).isTrue();

		Path v2 = migrationDir.resolve("V2__create_usuarios_table.sql");
		assertThat(v2).exists();
		assertThat(new ClassPathResource("db/migration/V2__create_usuarios_table.sql").exists()).isTrue();
		assertThat(Files.readString(v2))
				.contains("CREATE TABLE usuarios")
				.contains("CONSTRAINT uk_usuarios_email UNIQUE (email)")
				.contains("CONSTRAINT ck_usuarios_status CHECK")
				.contains("TIMESTAMPTZ")
				.contains("CREATE TABLE refresh_tokens")
				.contains("CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)")
				.contains("CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)");
	}
}
