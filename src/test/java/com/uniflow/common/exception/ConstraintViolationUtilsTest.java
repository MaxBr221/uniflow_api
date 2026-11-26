package com.uniflow.common.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintViolationUtilsTest {

	@Test
	void shouldReturnFalseWhenArgumentsAreNullOrBlank() {
		RuntimeException ex = new RuntimeException("duplicate key uk_usuarios_email");
		assertThat(ConstraintViolationUtils.containsConstraint(null, "uk_usuarios_email")).isFalse();
		assertThat(ConstraintViolationUtils.containsConstraint(ex, null)).isFalse();
		assertThat(ConstraintViolationUtils.containsConstraint(ex, "")).isFalse();
		assertThat(ConstraintViolationUtils.containsConstraint(ex, "   ")).isFalse();
	}

	@Test
	void shouldMatchHibernateConstraintNameCaseInsensitivelyEvenWithGenericMessages() {
		ConstraintViolationException hibernateViolation = new ConstraintViolationException(
				"violacao de integridade",
				new SQLException("erro generico", "23505"),
				"UK_USUARIOS_EMAIL"
		);
		DataIntegrityViolationException springException =
				new DataIntegrityViolationException("erro ao persistir", hibernateViolation);

		assertThat(ConstraintViolationUtils.containsConstraint(springException, "uk_usuarios_email")).isTrue();
		assertThat(ConstraintViolationUtils.containsConstraint(springException, "UK_USUARIOS_EMAIL")).isTrue();
		assertThat(ConstraintViolationUtils.containsConstraint(springException, "uk_semestres_atual")).isFalse();
	}

	@Test
	void shouldMatchConstraintNameInNestedCauseMessageAndIgnoreUnrelatedOrNullMessageChains() {
		SQLException rootCause = new SQLException(
				"ERROR: duplicate key value violates unique constraint \"uk_usuarios_email\""
		);
		DataIntegrityViolationException nested =
				new DataIntegrityViolationException("could not execute statement", new RuntimeException(null, rootCause));

		assertThat(ConstraintViolationUtils.containsConstraint(nested, "uk_usuarios_email")).isTrue();
		assertThat(ConstraintViolationUtils.containsConstraint(nested, "uk_refresh_tokens_token_hash")).isFalse();

		RuntimeException nullMessageChain = new RuntimeException(null, new IllegalStateException((String) null));
		assertThat(ConstraintViolationUtils.containsConstraint(nullMessageChain, "uk_usuarios_email")).isFalse();
	}
}
