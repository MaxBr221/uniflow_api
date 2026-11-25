package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRegistrationIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldPersistUsuarioInPostgreSqlWithBCryptHashAndRejectCaseInsensitiveDuplicate() {
		RegisterRequest primeiroCadastro = new RegisterRequest(
				"  Mariana Costa  ",
				"  Mariana.Costa@UniFlow.EDU ",
				"UniFlow@2026"
		);

		AuthUserResponse response = authService.register(primeiroCadastro);

		assertThat(response.id()).isNotNull();
		assertThat(response.nome()).isEqualTo("Mariana Costa");
		assertThat(response.email()).isEqualTo("mariana.costa@uniflow.edu");
		assertThat(response.status()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.updatedAt()).isNotNull();

		Usuario persistido = usuarioRepository.findByEmailIgnoreCase("MARIANA.COSTA@UNIFLOW.EDU").orElseThrow();
		assertThat(persistido.getId()).isEqualTo(response.id());
		assertThat(persistido.getEmail()).isEqualTo("mariana.costa@uniflow.edu");
		assertThat(persistido.getSenhaHash()).startsWith("$2a$10$");
		assertThat(persistido.getSenhaHash()).isNotEqualTo("UniFlow@2026");
		assertThat(passwordEncoder.matches("UniFlow@2026", persistido.getSenhaHash())).isTrue();

		RegisterRequest cadastroDuplicado = new RegisterRequest(
				"Outra Pessoa",
				"MARIANA.COSTA@uniflow.edu",
				"OutraSenha@2026"
		);

		assertThatThrownBy(() -> authService.register(cadastroDuplicado))
				.isInstanceOf(ConflictException.class);
		assertThat(usuarioRepository.count()).isEqualTo(1);
	}
}
