package com.uniflow.usuario.repository;

import com.uniflow.TestcontainersConfiguration;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class UsuarioRepositoryIntegrationTest {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private TestEntityManager testEntityManager;

	@Test
	void shouldPersistAndReloadUsuarioWithNormalizedEmailAndAuditTimestamps() {
		Usuario usuario = new Usuario("  Ana Silva  ", "  Ana.Silva@UniFlow.EDU  ", "$2a$10$hashExemploSeguro");

		Usuario salvo = usuarioRepository.saveAndFlush(usuario);
		testEntityManager.clear();

		Usuario encontrado = usuarioRepository.findById(salvo.getId()).orElseThrow();
		assertThat(encontrado.getId()).isNotNull();
		assertThat(encontrado.getNome()).isEqualTo("Ana Silva");
		assertThat(encontrado.getEmail()).isEqualTo("ana.silva@uniflow.edu");
		assertThat(encontrado.getSenhaHash()).isEqualTo("$2a$10$hashExemploSeguro");
		assertThat(encontrado.getStatus()).isEqualTo(StatusUsuario.ATIVO);
		assertThat(encontrado.podeAutenticar()).isTrue();
		assertThat(encontrado.getCreatedAt()).isNotNull();
		assertThat(encontrado.getUpdatedAt()).isNotNull();
	}

	@Test
	void shouldFindByEmailIgnoreCaseRegardlessOfLetterCase() {
		Usuario salvo = usuarioRepository.saveAndFlush(
				new Usuario("Carlos Lima", "carlos.lima@uniflow.edu", "$2a$10$hashCarlosExemplo")
		);
		testEntityManager.clear();

		Optional<Usuario> porMaiusculo = usuarioRepository.findByEmailIgnoreCase("CARLOS.LIMA@UNIFLOW.EDU");
		Optional<Usuario> porMisto = usuarioRepository.findByEmailIgnoreCase("Carlos.Lima@UniFlow.Edu");
		Optional<Usuario> inexistente = usuarioRepository.findByEmailIgnoreCase("nao.existe@uniflow.edu");

		assertThat(porMaiusculo)
				.isPresent()
				.get()
				.satisfies(u -> {
					assertThat(u.getId()).isEqualTo(salvo.getId());
					assertThat(u.getEmail()).isEqualTo("carlos.lima@uniflow.edu");
				});
		assertThat(porMisto)
				.isPresent()
				.get()
				.extracting(Usuario::getId)
				.isEqualTo(salvo.getId());
		assertThat(inexistente).isEmpty();
	}

	@Test
	void shouldCheckExistsByEmailIgnoreCaseTrueAndFalse() {
		usuarioRepository.saveAndFlush(
				new Usuario("Mariana Alves", "mariana@uniflow.edu", "$2a$10$hashMarianaExemplo")
		);
		testEntityManager.clear();

		assertThat(usuarioRepository.existsByEmailIgnoreCase("mariana@uniflow.edu")).isTrue();
		assertThat(usuarioRepository.existsByEmailIgnoreCase("MARIANA@UNIFLOW.EDU")).isTrue();
		assertThat(usuarioRepository.existsByEmailIgnoreCase("MaRiAnA@UniFlow.Edu")).isTrue();
		assertThat(usuarioRepository.existsByEmailIgnoreCase("outra.pessoa@uniflow.edu")).isFalse();
	}

	@Test
	void shouldUpdateStatusAndUpdatedAtWhilePreservingCreatedAt() {
		Usuario salvo = usuarioRepository.saveAndFlush(
				new Usuario("Joao Pedro", "joao.pedro@uniflow.edu", "$2a$10$hashJoaoExemplo")
		);
		testEntityManager.clear();

		Usuario paraAtualizar = usuarioRepository.findById(salvo.getId()).orElseThrow();
		Instant createdAtOriginal = paraAtualizar.getCreatedAt();
		paraAtualizar.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(paraAtualizar);
		testEntityManager.clear();

		Usuario atualizado = usuarioRepository.findById(salvo.getId()).orElseThrow();
		assertThat(atualizado.getStatus()).isEqualTo(StatusUsuario.BLOQUEADO);
		assertThat(atualizado.podeAutenticar()).isFalse();
		assertThat(atualizado.getCreatedAt()).isEqualTo(createdAtOriginal);
		assertThat(atualizado.getUpdatedAt()).isAfterOrEqualTo(createdAtOriginal);
	}

	@Test
	void shouldRejectDuplicateEmailDifferingOnlyByCaseViaUniqueConstraint() {
		usuarioRepository.saveAndFlush(
				new Usuario("Primeiro Usuario", "duplicado@uniflow.edu", "$2a$10$hashPrimeiro")
		);
		testEntityManager.clear();

		Usuario duplicado = new Usuario("Segundo Usuario", "DUPLICADO@UNIFLOW.EDU", "$2a$10$hashSegundo");

		assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicado))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
