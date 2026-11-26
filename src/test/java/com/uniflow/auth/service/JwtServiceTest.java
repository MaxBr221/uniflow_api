package com.uniflow.auth.service;

import com.uniflow.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

	private static final String TEST_SECRET = "uniflow-jwt-unit-test-secret-key-at-least-32-bytes";
	private static final String OTHER_SECRET = "another-totally-different-secret-key-at-least-32-bytes";

	@Test
	void shouldGenerateSignedAccessTokenWithSubJtiIatExpAndNoSensitiveFields() {
		Instant fixedNow = Instant.parse("2026-11-25T12:00:00Z");
		Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
		JwtService jwtService = new JwtService(TEST_SECRET, 900_000L, fixedClock);

		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana.silva@uniflow.edu", "$2a$10$hashSecretoNaoPodeVazar");
		usuario.setId(userId);

		String token1 = jwtService.generateAccessToken(usuario);
		String token2 = jwtService.generateAccessToken(usuario);

		assertThat(token1).isNotBlank().isNotEqualTo(token2);
		assertThat(jwtService.getAccessExpirationSeconds()).isEqualTo(900L);

		Claims claims = jwtService.parseAndValidateAccessToken(token1);
		assertThat(claims.getSubject()).isEqualTo(userId.toString());
		assertThat(claims.getId()).isNotBlank();
		assertThat(UUID.fromString(claims.getId())).isNotNull();
		assertThat(claims.getIssuedAt()).isEqualTo(Date.from(fixedNow));
		assertThat(claims.getExpiration()).isEqualTo(Date.from(fixedNow.plusMillis(900_000L)));
		assertThat(claims.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("ana.silva@uniflow.edu");
		assertThat(claims.get(JwtService.CLAIM_TOKEN_TYPE, String.class)).isEqualTo(JwtService.TOKEN_TYPE_ACCESS);
		assertThat(claims).doesNotContainKeys("senha", "senhaHash", "password");
		assertThat(jwtService.extractUserId(token1)).isEqualTo(userId);
	}

	@Test
	void shouldRejectExpiredAccessToken() {
		Instant issuedAt = Instant.parse("2026-11-25T12:00:00Z");
		JwtService issuer = new JwtService(TEST_SECRET, 60_000L, Clock.fixed(issuedAt, ZoneOffset.UTC));
		Usuario usuario = new Usuario("Carlos Lima", "carlos@uniflow.edu", "$2a$10$hashExemplo");
		usuario.setId(UUID.randomUUID());

		String token = issuer.generateAccessToken(usuario);

		Instant afterExpiration = issuedAt.plusSeconds(61);
		JwtService verifier = new JwtService(TEST_SECRET, 60_000L, Clock.fixed(afterExpiration, ZoneOffset.UTC));

		assertThatThrownBy(() -> verifier.parseAndValidateAccessToken(token))
				.isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void shouldRejectTokenSignedWithDifferentSecretOrWrongTypeOrMissingJti() {
		Instant fixedNow = Instant.parse("2026-11-25T12:00:00Z");
		Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
		JwtService issuer = new JwtService(TEST_SECRET, 900_000L, fixedClock);
		JwtService otherVerifier = new JwtService(OTHER_SECRET, 900_000L, fixedClock);

		Usuario usuario = new Usuario("Mariana Costa", "mariana@uniflow.edu", "$2a$10$hashExemplo");
		usuario.setId(UUID.randomUUID());
		String validToken = issuer.generateAccessToken(usuario);

		assertThatThrownBy(() -> otherVerifier.parseAndValidateAccessToken(validToken))
				.isInstanceOf(SignatureException.class);

		SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
		String wrongTypeToken = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(usuario.getId().toString())
				.claim(JwtService.CLAIM_TOKEN_TYPE, "refresh")
				.issuedAt(Date.from(fixedNow))
				.expiration(Date.from(fixedNow.plusSeconds(300)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();

		assertThatThrownBy(() -> issuer.parseAndValidateAccessToken(wrongTypeToken))
				.isInstanceOf(JwtException.class);

		String missingJtiToken = Jwts.builder()
				.subject(usuario.getId().toString())
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(fixedNow))
				.expiration(Date.from(fixedNow.plusSeconds(300)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();

		assertThatThrownBy(() -> issuer.parseAndValidateAccessToken(missingJtiToken))
				.isInstanceOf(JwtException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "curto-demais"})
	void shouldFailFastWhenSecretIsBlankOrShorterThan32Bytes(String invalidSecret) {
		assertThatThrownBy(() -> new JwtService(invalidSecret, 900_000L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldFailFastWhenSecretIsNullOrExpirationIsInvalid() {
		assertThatThrownBy(() -> new JwtService(null, 900_000L))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new JwtService(TEST_SECRET, 999L))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new JwtService(TEST_SECRET, 0L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectBlankUnsignedMalformedMissingSubOrInvalidUuidSubTokensAndNullUserOnGenerate() {
		Instant fixedNow = Instant.parse("2026-11-25T12:00:00Z");
		JwtService jwtService = new JwtService(TEST_SECRET, 900_000L, Clock.fixed(fixedNow, ZoneOffset.UTC));
		SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> jwtService.generateAccessToken(null))
				.isInstanceOf(NullPointerException.class);
		Usuario semId = new Usuario("Sem Id", "sem.id@uniflow.edu", "$2a$10$hash");
		assertThatThrownBy(() -> jwtService.generateAccessToken(semId))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(null))
				.isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken("   "))
				.isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken("nao.e.um.jwt.valido"))
				.isInstanceOf(JwtException.class);

		// Ataque classico alg=none (token nao assinado)
		String unsignedAlgNone = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
				+ "."
				+ java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(("{\"sub\":\"" + UUID.randomUUID() + "\",\"jti\":\"" + UUID.randomUUID()
						+ "\",\"type\":\"access\"}").getBytes(StandardCharsets.UTF_8))
				+ ".";
		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(unsignedAlgNone))
				.isInstanceOf(JwtException.class);

		String blankJtiToken = Jwts.builder()
				.id("   ")
				.subject(UUID.randomUUID().toString())
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(fixedNow))
				.expiration(Date.from(fixedNow.plusSeconds(300)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(blankJtiToken))
				.isInstanceOf(JwtException.class);

		String missingSubToken = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(fixedNow))
				.expiration(Date.from(fixedNow.plusSeconds(300)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
		assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(missingSubToken))
				.isInstanceOf(JwtException.class);

		String nonUuidSubToken = Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject("usuario-nao-uuid")
				.claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
				.issuedAt(Date.from(fixedNow))
				.expiration(Date.from(fixedNow.plusSeconds(300)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
		assertThatThrownBy(() -> jwtService.extractUserId(nonUuidSubToken))
				.isInstanceOf(JwtException.class);
	}
}
