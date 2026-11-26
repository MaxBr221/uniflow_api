package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthUserResponse;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.common.exception.ConflictException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthRegistrationIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

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
		assertThat(refreshTokenRepository.count()).isZero();

		RegisterRequest cadastroDuplicado = new RegisterRequest(
				"Outra Pessoa",
				"MARIANA.COSTA@uniflow.edu",
				"OutraSenha@2026"
		);

		assertThatThrownBy(() -> authService.register(cadastroDuplicado))
				.isInstanceOf(ConflictException.class);
		assertThat(usuarioRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldValidateRegistrationOverHttpRejectDuplicatesAndHandleConcurrentEmailRaceWithout500() throws Exception {
		// 1. Payload invalido retorna 400 VALIDATION_ERROR RFC 7807 sem persistir nada no PostgreSQL
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": " ",
								  "email": "invalido",
								  "senha": "curta"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[*].field", hasItems("nome", "email", "senha")));
		assertThat(usuarioRepository.count()).isZero();

		// 2. Corrida real concorrente no mesmo e-mail (variando caixa/espacos) contra PostgreSQL:
		// exatamente 1 requisicao cria o registro (201) e todas as demais retornam 409 RESOURCE_CONFLICT (nunca 500)
		int concurrentCallers = 6;
		String[] emailVariants = {
				"concorrente@uniflow.edu",
				"CONCORRENTE@UNIFLOW.EDU",
				"  Concorrente@UniFlow.Edu  ",
				"concorrente@UNIFLOW.edu",
				"  CONCORRENTE@uniflow.edu",
				"Concorrente@uniflow.EDU  "
		};

		CountDownLatch ready = new CountDownLatch(concurrentCallers);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(concurrentCallers);

		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (int i = 0; i < concurrentCallers; i++) {
				String variant = emailVariants[i];
				int idx = i;
				Callable<Integer> task = () -> {
					ready.countDown();
					start.await(5, TimeUnit.SECONDS);
					return mockMvc.perform(post("/api/v1/auth/register")
									.contentType(MediaType.APPLICATION_JSON)
									.content("""
											{
											  "nome": "Estudante Concorrente %d",
											  "email": "%s",
											  "senha": "UniFlow@2026"
											}
											""".formatted(idx, variant)))
							.andReturn()
							.getResponse()
							.getStatus();
				};
				futures.add(pool.submit(task));
			}

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> future : futures) {
				statuses.add(future.get(15, TimeUnit.SECONDS));
			}

			assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
			assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(concurrentCallers - 1);
			assertThat(usuarioRepository.count()).isEqualTo(1);
		} finally {
			pool.shutdownNow();
		}

		// 3. Nova tentativa sequencial de duplicidade retorna ProblemDetail 409 completo
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nome": "Outro Nome",
								  "email": "CONCORRENTE@UNIFLOW.EDU",
								  "senha": "OutraSenha@2026"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.uniflow.dev/errors/resource-conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
				.andExpect(jsonPath("$.timestamp", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")))
				.andExpect(jsonPath("$.fieldErrors", empty()));

		assertThat(usuarioRepository.count()).isEqualTo(1);
	}
}
