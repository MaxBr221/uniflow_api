package com.uniflow.auth.service;

import com.uniflow.AbstractIntegrationTest;
import com.uniflow.auth.dto.AuthTokenResponse;
import com.uniflow.auth.dto.LoginRequest;
import com.uniflow.auth.dto.RegisterRequest;
import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import com.uniflow.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@BeforeEach
	void setUp() {
		usuarioRepository.deleteAllInBatch();
	}

	@Test
	void shouldPersistHashedRefreshTokenOnLoginAndRotateInvalidatingPreviousToken() {
		AuthTokenResponse login = registerAndLogin("ana.rotacao@uniflow.edu");
		UUID userId = login.usuario().id();
		String token1 = login.refreshToken();

		assertThat(token1).startsWith(RefreshTokenService.TOKEN_PREFIX);
		RefreshToken row1Before = refreshTokenRepository.findByTokenHash(RefreshTokenService.hashToken(token1))
				.orElseThrow();
		assertThat(row1Before.getTokenHash()).hasSize(64);
		assertThat(row1Before.getTokenHash()).isNotEqualTo(token1);
		assertThat(row1Before.isRevoked()).isFalse();
		assertThat(row1Before.getReplacedByTokenId()).isNull();
		assertThat(row1Before.getExpiresAt()).isAfter(row1Before.getCreatedAt());

		RefreshTokenService.RotatedRefreshToken rotation1 = refreshTokenService.rotate(token1);
		String token2 = rotation1.newRawToken();
		assertThat(token2).isNotEqualTo(token1);

		RefreshToken row1After = refreshTokenRepository.findById(row1Before.getId()).orElseThrow();
		RefreshToken row2 = refreshTokenRepository.findById(rotation1.newEntity().getId()).orElseThrow();

		assertThat(row1After.isRevoked()).isTrue();
		assertThat(row1After.getRevokedAt()).isNotNull();
		assertThat(row1After.getReplacedByTokenId()).isEqualTo(row2.getId());
		assertThat(row2.isRevoked()).isFalse();
		assertThat(row2.getReplacedByTokenId()).isNull();

		RefreshTokenService.RotatedRefreshToken rotation2 = refreshTokenService.rotate(token2);
		RefreshToken row2After = refreshTokenRepository.findById(row2.getId()).orElseThrow();
		assertThat(row2After.isRevoked()).isTrue();
		assertThat(row2After.getReplacedByTokenId()).isEqualTo(rotation2.newEntity().getId());
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(1);
	}

	@Test
	void shouldDetectRotatedTokenReuseAndRevokeAllActiveTokensInDatabaseWithoutRollback() {
		AuthTokenResponse session1 = registerAndLogin("carlos.reuso@uniflow.edu");
		UUID userId = session1.usuario().id();
		Usuario usuario = usuarioRepository.findById(userId).orElseThrow();

		String token1 = session1.refreshToken();
		RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(token1);
		String activeTokenFromSession1 = rotated.newRawToken();

		String activeTokenFromSession2 = refreshTokenService.issue(usuario).rawToken();
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isEqualTo(2);

		// Reutilizacao do token1 ja rotacionado deve falhar e revogar TODOS os tokens ativos do usuario
		assertThatThrownBy(() -> refreshTokenService.rotate(token1))
				.isInstanceOf(AuthenticationFailedException.class);

		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isZero();

		assertThatThrownBy(() -> refreshTokenService.rotate(activeTokenFromSession1))
				.isInstanceOf(AuthenticationFailedException.class);
		assertThatThrownBy(() -> refreshTokenService.rotate(activeTokenFromSession2))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void shouldRejectExpiredOrBlockedUserTokensAndEnforceUniqueTokenHashConstraint() {
		AuthTokenResponse login = registerAndLogin("diego.bloqueio@uniflow.edu");
		UUID userId = login.usuario().id();
		Usuario usuario = usuarioRepository.findById(userId).orElseThrow();

		String expiredRaw = "rft_token_expirado_manual";
		RefreshToken expired = new RefreshToken(
				usuario,
				RefreshTokenService.hashToken(expiredRaw),
				Instant.now().minusSeconds(30),
				Instant.now().minusSeconds(120)
		);
		RefreshToken savedExpired = refreshTokenRepository.saveAndFlush(expired);

		assertThatThrownBy(() -> refreshTokenService.rotate(expiredRaw))
				.isInstanceOf(AuthenticationFailedException.class);
		assertThat(refreshTokenRepository.findById(savedExpired.getId()).orElseThrow().isRevoked()).isTrue();

		// Tentativa de violar uk_refresh_tokens_token_hash no PostgreSQL
		RefreshToken duplicateHash = new RefreshToken(
				usuario,
				savedExpired.getTokenHash(),
				Instant.now().plusSeconds(3600),
				Instant.now()
		);
		assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(duplicateHash))
				.isInstanceOf(DataIntegrityViolationException.class);

		// Conta bloqueada deve revogar tokens ativos ao tentar rotacionar
		usuario.setStatus(StatusUsuario.BLOQUEADO);
		usuarioRepository.saveAndFlush(usuario);

		assertThatThrownBy(() -> refreshTokenService.rotate(login.refreshToken()))
				.isInstanceOf(AuthenticationFailedException.class);
		assertThat(refreshTokenRepository.countByUsuarioIdAndRevokedFalse(userId)).isZero();
	}

	@Test
	void shouldAllowOnlyOneSuccessfulRotationUnderConcurrentRequestsForSameToken() throws Exception {
		AuthTokenResponse login = registerAndLogin("elena.concorrencia@uniflow.edu");
		UUID userId = login.usuario().id();
		String sharedToken = login.refreshToken();

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);

		Callable<Boolean> attemptRotate = () -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			try {
				refreshTokenService.rotate(sharedToken);
				return true;
			} catch (AuthenticationFailedException ex) {
				return false;
			}
		};

		try {
			Future<Boolean> first = pool.submit(attemptRotate);
			Future<Boolean> second = pool.submit(attemptRotate);
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			List<Boolean> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
			assertThat(results).containsExactlyInAnyOrder(true, false);

			List<RefreshToken> allTokens = refreshTokenRepository.findAllByUsuarioIdOrderByCreatedAtDesc(userId);
			assertThat(allTokens).hasSize(2);
			assertThat(allTokens).allMatch(RefreshToken::isRevoked);
		} finally {
			pool.shutdownNow();
		}
	}

	private AuthTokenResponse registerAndLogin(String email) {
		authService.register(new RegisterRequest("Estudante Teste", email, "UniFlow@2026"));
		return authService.login(new LoginRequest(email, "UniFlow@2026"));
	}
}
