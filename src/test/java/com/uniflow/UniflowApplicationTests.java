package com.uniflow;

import com.uniflow.common.exception.GlobalExceptionHandler;
import com.uniflow.common.filter.RequestLoggingFilter;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.validation.Validator;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class UniflowApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Autowired
	private OpenAPI openAPI;

	@Autowired
	private Validator validator;

	@Autowired
	private GlobalExceptionHandler globalExceptionHandler;

	@Autowired
	private RequestLoggingFilter requestLoggingFilter;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoadsWithPostgreSqlFlywayAndCoreInfrastructureBeans() throws Exception {
		assertThat(dataSource).isNotNull();
		assertThat(flyway.info().current()).isNotNull();
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(Integer.parseInt(flyway.info().current().getVersion().getVersion())).isGreaterThanOrEqualTo(2);
		assertThat(openAPI).isNotNull();
		assertThat(validator).isNotNull();
		assertThat(globalExceptionHandler).isNotNull();
		assertThat(requestLoggingFilter).isNotNull();
		assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
		String encoded = passwordEncoder.encode("UniFlow@2026");
		assertThat(passwordEncoder.matches("UniFlow@2026", encoded)).isTrue();
		assertThat(passwordEncoder.matches("SenhaIncorreta@2026", encoded)).isFalse();

		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement()) {
			try (ResultSet rs = statement.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'pgcrypto'")) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getString("extname")).isEqualTo("pgcrypto");
			}
			try (ResultSet rs = statement.executeQuery(
					"SELECT COUNT(*) AS total FROM information_schema.tables "
							+ "WHERE table_schema = 'public' AND table_name IN ('usuarios', 'refresh_tokens')")) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getInt("total")).isEqualTo(2);
			}
		}
	}
}
