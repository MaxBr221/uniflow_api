package com.uniflow.auth.service;

import com.uniflow.auth.entity.RefreshToken;
import com.uniflow.auth.repository.RefreshTokenRepository;
import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.entity.Usuario;
import com.uniflow.usuario.enums.StatusUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final long EXPIRATION_MS = 604_800_000L;
	private static final Instant FIXED_NOW = Instant.parse("2026-11-25T14:00:00Z");

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, EXPIRATION_MS, fixedClock);
	}

	@Test
	void shouldValidateConstructorConfiguration() {
		Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
		assertThatThrownBy(() -> new RefreshTokenService(refreshTokenRepository, 999L, fixedClock))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldIssueHashedOpaqueRefreshTokenAndRedactSecretsInToString() {
		Usuario usuario = activeUsuario();
		when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class))).thenAnswer(invocation -> {
			RefreshToken entity = invocation.getArgument(0);
			entity.setId(UUID.randomUUID());
			return entity;
		});

		RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(usuario);

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).saveAndFlush(captor.capture());
		RefreshToken persisted = captor.getValue();

		assertThat(issued.rawToken()).startsWith(RefreshTokenService.TOKEN_PREFIX);
		assertThat(issued.rawToken().length()).isGreaterThan(40);
		assertThat(persisted.getTokenHash()).hasSize(64);
		assertThat(persisted.getTokenHash()).matches("^[0-9a-f]{64}$");
		assertThat(persisted.getTokenHash()).isEqualTo(RefreshTokenService.hashToken(issued.rawToken()));
		assertThat(persisted.getTokenHash()).isNotEqualTo(issued.rawToken());
		assertThat(persisted.getCreatedAt()).isEqualTo(FIXED_NOW);
		assertThat(persisted.getExpiresAt()).isEqualTo(FIXED_NOW.plusMillis(EXPIRATION_MS));
		assertThat(persisted.isRevoked()).isFalse();
		assertThat(issued.toString()).doesNotContain(issued.rawToken());
	}

	@ParameterizedTest
	@EnumSource(value = StatusUsuario.class, names = {"INATIVO", "BLOQUEADO", "EXCLUIDO"})
	void shouldRejectIssueWhenUsuarioStatusDisallowsAuthentication(StatusUsuario status) {
		Usuario usuario = activeUsuario();
		usuario.setStatus(status);

		assertThatThrownBy(() -> refreshTokenService.issue(usuario))
				.isInstanceOf(AuthenticationFailedException.class);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	void shouldRotateValidRefreshTokenAndInvalidatePreviousToken() {
		Usuario usuario = activeUsuario();
		String currentRawToken = RefreshTokenService.generateRawToken();
		String currentHash = RefreshTokenService.hashToken(currentRawToken);
		UUID currentId = UUID.randomUUID();
		UUID replacementId = UUID.randomUUID();

		RefreshToken current = new RefreshToken(
				usuario,
				currentHash,
				FIXED_NOW.plusSeconds(3600),
				FIXED_NOW.minusSeconds(60)
		);
		current.setId(currentId);

		when(refreshTokenRepository.findByTokenHashForUpdate(currentHash)).thenReturn(Optional.of(current));
		when(refreshTokenRepository.saveAndFlush(any(RefreshToken.class))).thenAnswer(invocation -> {
			RefreshToken entity = invocation.getArgument(0);
			if (entity.getId() == null) {
				entity.setId(replacementId);
			}
			return entity;
		});

		RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(currentRawToken);

		assertThat(rotated.usuario()).isSameAs(usuario);
		assertThat(rotated.newRawToken()).startsWith(RefreshTokenService.TOKEN_PREFIX);
		assertThat(rotated.newRawToken()).isNotEqualTo(currentRawToken);
		assertThat(rotated.newEntity().getId()).isEqualTo(replacementId);
		assertThat(rotated.newEntity().getTokenHash()).isEqualTo(RefreshTokenService.hashToken(rotated.newRawToken()));
		assertThat(current.isRevoked()).isTrue();
		assertThat(current.getRevokedAt()).isEqualTo(FIXED_NOW);
		assertThat(current.getReplacedByTokenId()).isEqualTo(replacementId);
		assertThat(rotated.toString()).doesNotContain(rotated.newRawToken());
	}

	@Test
	void shouldRejectBlankOrUnknownRefreshTokenWithoutLookupSideEffects() {
		assertThatThrownBy(() -> refreshTokenService.rotate("   "))
				.isInstanceOf(AuthenticationFailedException.class);
		verifyNoInteractions(refreshTokenRepository);

		String unknownRaw = "rft_desconhecido";
		String unknownHash = RefreshTokenService.hashToken(unknownRaw);
		when(refreshTokenRepository.findByTokenHashForUpdate(unknownHash)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.rotate(unknownRaw))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void shouldDetectReuseOfRevokedRefreshTokenAndRevokeAllActiveTokensForUsuario() {
		Usuario usuario = activeUsuario();
		String reusedRaw = "rft_reutilizado";
		String reusedHash = RefreshTokenService.hashToken(reusedRaw);

		RefreshToken revokedToken = new RefreshToken(
				usuario,
				reusedHash,
				FIXED_NOW.plusSeconds(3600),
				FIXED_NOW.minusSeconds(300)
		);
		revokedToken.setId(UUID.randomUUID());
		revokedToken.markRotated(UUID.randomUUID(), FIXED_NOW.minusSeconds(10));

		when(refreshTokenRepository.findByTokenHashForUpdate(reusedHash)).thenReturn(Optional.of(revokedToken));

		assertThatThrownBy(() -> refreshTokenService.rotate(reusedRaw))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenRepository).revokeAllActiveByUsuarioId(usuario.getId(), FIXED_NOW);
	}

	@Test
	void shouldRevokeExpiredRefreshTokenAndThrowAuthenticationFailedException() {
		Usuario usuario = activeUsuario();
		String expiredRaw = "rft_expirado";
		String expiredHash = RefreshTokenService.hashToken(expiredRaw);

		RefreshToken expiredToken = new RefreshToken(
				usuario,
				expiredHash,
				FIXED_NOW.minusSeconds(1),
				FIXED_NOW.minusSeconds(3600)
		);
		expiredToken.setId(UUID.randomUUID());

		when(refreshTokenRepository.findByTokenHashForUpdate(expiredHash)).thenReturn(Optional.of(expiredToken));

		assertThatThrownBy(() -> refreshTokenService.rotate(expiredRaw))
				.isInstanceOf(AuthenticationFailedException.class);

		assertThat(expiredToken.isRevoked()).isTrue();
		assertThat(expiredToken.getRevokedAt()).isEqualTo(FIXED_NOW);
		verify(refreshTokenRepository).saveAndFlush(expiredToken);
	}

	@ParameterizedTest
	@EnumSource(value = StatusUsuario.class, names = {"INATIVO", "BLOQUEADO", "EXCLUIDO"})
	void shouldRevokeActiveTokensAndRejectRotationWhenUsuarioStatusDisallowsLogin(StatusUsuario blockedStatus) {
		Usuario usuario = activeUsuario();
		usuario.setStatus(blockedStatus);

		String rawToken = "rft_usuario_bloqueado";
		String hash = RefreshTokenService.hashToken(rawToken);
		RefreshToken token = new RefreshToken(
				usuario,
				hash,
				FIXED_NOW.plusSeconds(3600),
				FIXED_NOW.minusSeconds(60)
		);
		token.setId(UUID.randomUUID());

		when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenRepository).revokeAllActiveByUsuarioId(usuario.getId(), FIXED_NOW);
	}

	@Test
	void shouldDelegateRevokeAllForUserUsingClockInstant() {
		UUID userId = UUID.randomUUID();
		when(refreshTokenRepository.revokeAllActiveByUsuarioId(userId, FIXED_NOW)).thenReturn(2);

		int revokedCount = refreshTokenService.revokeAllForUser(userId);

		assertThat(revokedCount).isEqualTo(2);
		verify(refreshTokenRepository).revokeAllActiveByUsuarioId(userId, FIXED_NOW);
	}

	@Test
	void shouldRevokeOwnedActiveRefreshTokenInRevokeTokenForUser() {
		Usuario owner = activeUsuario();
		String rawToken = "rft_meu_token_ativo";
		String hash = RefreshTokenService.hashToken(rawToken);
		RefreshToken token = new RefreshToken(owner, hash, FIXED_NOW.plusSeconds(3600), FIXED_NOW.minusSeconds(30));
		token.setId(UUID.randomUUID());

		when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(token));

		int revoked = refreshTokenService.revokeTokenForUser(owner.getId(), "  " + rawToken + "  ");

		assertThat(revoked).isEqualTo(1);
		assertThat(token.isRevoked()).isTrue();
		assertThat(token.getRevokedAt()).isEqualTo(FIXED_NOW);
		verify(refreshTokenRepository).saveAndFlush(token);
	}

	@Test
	void shouldBeIdempotentWhenTokenIsAbsentOrAlreadyRevokedByOwner() {
		UUID ownerId = UUID.randomUUID();
		String absentHash = RefreshTokenService.hashToken("rft_ausente");
		when(refreshTokenRepository.findByTokenHashForUpdate(absentHash)).thenReturn(Optional.empty());
		assertThat(refreshTokenService.revokeTokenForUser(ownerId, "rft_ausente")).isZero();

		Usuario owner = activeUsuario();
		String alreadyRevokedRaw = "rft_ja_revogado";
		String alreadyRevokedHash = RefreshTokenService.hashToken(alreadyRevokedRaw);
		RefreshToken revokedToken = new RefreshToken(
				owner,
				alreadyRevokedHash,
				FIXED_NOW.plusSeconds(3600),
				FIXED_NOW.minusSeconds(60)
		);
		revokedToken.setId(UUID.randomUUID());
		revokedToken.revoke(FIXED_NOW.minusSeconds(10));
		when(refreshTokenRepository.findByTokenHashForUpdate(alreadyRevokedHash)).thenReturn(Optional.of(revokedToken));

		assertThat(refreshTokenService.revokeTokenForUser(owner.getId(), alreadyRevokedRaw)).isZero();
		verify(refreshTokenRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldThrowUnauthorizedOperationWithoutModifyingWhenTokenBelongsToAnotherUser() {
		Usuario owner = activeUsuario();
		UUID attackerId = UUID.randomUUID();
		String victimsRaw = "rft_de_outro_usuario";
		String victimsHash = RefreshTokenService.hashToken(victimsRaw);
		RefreshToken victimsToken = new RefreshToken(
				owner,
				victimsHash,
				FIXED_NOW.plusSeconds(3600),
				FIXED_NOW.minusSeconds(30)
		);
		victimsToken.setId(UUID.randomUUID());

		when(refreshTokenRepository.findByTokenHashForUpdate(victimsHash)).thenReturn(Optional.of(victimsToken));

		assertThatThrownBy(() -> refreshTokenService.revokeTokenForUser(attackerId, victimsRaw))
				.isInstanceOf(UnauthorizedOperationException.class);

		assertThat(victimsToken.isRevoked()).isFalse();
		verify(refreshTokenRepository, never()).saveAndFlush(any());
	}

	private static Usuario activeUsuario() {
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSalvo");
		usuario.setId(UUID.randomUUID());
		return usuario;
	}
}
