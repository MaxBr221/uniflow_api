package com.uniflow.auth.repository;

import com.uniflow.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para gerenciamento de refresh tokens e deteccao/revogacao de sessoes.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<RefreshToken> findAllByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

	long countByUsuarioIdAndRevokedFalse(UUID usuarioId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshToken rt
			set rt.revoked = true, rt.revokedAt = :revokedAt
			where rt.usuario.id = :usuarioId and rt.revoked = false
			""")
	int revokeAllActiveByUsuarioId(@Param("usuarioId") UUID usuarioId, @Param("revokedAt") Instant revokedAt);
}
