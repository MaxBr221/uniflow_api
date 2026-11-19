package com.uniflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModularStructureTest {

	private static final List<String> DOMAIN_MODULES = List.of(
			"auth",
			"usuario",
			"semestre",
			"disciplina",
			"aula",
			"atividade",
			"calendario",
			"trabalho",
			"grupo",
			"arquivo",
			"notificacao",
			"planejamento",
			"assinatura"
	);

	private static final List<String> FULL_LAYERED_MODULES = List.of(
			"auth",
			"usuario",
			"semestre",
			"disciplina",
			"aula",
			"atividade",
			"trabalho",
			"grupo",
			"arquivo",
			"notificacao",
			"planejamento",
			"assinatura"
	);

	private static final List<String> STANDARD_SUBPACKAGES = List.of(
			"controller",
			"service",
			"repository",
			"entity",
			"dto",
			"mapper",
			"enums"
	);

	@Test
	void rootApplicationMustLiveInComUniflowForModularComponentScan() {
		assertThat(UniflowApplication.class.getPackageName()).isEqualTo("com.uniflow");
		assertThat(UniflowApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
		assertThat(Path.of("src/main/java/com/uniflow/api")).doesNotExist();
		assertThat(Path.of("src/test/java/com/uniflow/api")).doesNotExist();
	}

	@Test
	void allModularMonolithDomainAndInfraPackagesMustExist() {
		Path base = Path.of("src/main/java/com/uniflow");

		for (String module : DOMAIN_MODULES) {
			Path moduleDir = base.resolve(module);
			assertThat(moduleDir).as("Modulo '%s' deve existir", module).exists().isDirectory();
			assertThat(moduleDir.resolve("package-info.java")).exists();
		}

		for (String module : FULL_LAYERED_MODULES) {
			Path moduleDir = base.resolve(module);
			for (String layer : STANDARD_SUBPACKAGES) {
				assertThat(moduleDir.resolve(layer))
						.as("Subpacote '%s.%s' deve existir", module, layer)
						.exists()
						.isDirectory();
			}
		}

		// Calendario consolida dados existentes sem entidades/repositorios proprios (MOD-07)
		Path calendarioDir = base.resolve("calendario");
		for (String layer : List.of("controller", "service", "dto", "mapper", "enums")) {
			assertThat(calendarioDir.resolve(layer)).exists().isDirectory();
		}

		for (String infra : List.of("common", "config")) {
			Path infraDir = base.resolve(infra);
			assertThat(infraDir).exists().isDirectory();
			assertThat(infraDir.resolve("package-info.java")).exists();
		}
		assertThat(base.resolve("common/exception")).exists().isDirectory();
		assertThat(base.resolve("common/dto")).exists().isDirectory();
		assertThat(base.resolve("common/filter")).exists().isDirectory();
	}
}
