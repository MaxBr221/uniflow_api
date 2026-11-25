package com.uniflow.auth.entity;

import com.uniflow.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA para persistencia segura de refresh tokens via hash e rastreamento de rotacao.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
		name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "usuario_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_refresh_tokens_usuario")
	)
	private Usuario usuario;

	@Column(name = "token_hash", nullable = false, length = 128, updatable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "revoked", nullable = false)
	private boolean revoked = false;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id")
	private UUID replacedByTokenId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public RefreshToken(Usuario usuario, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.usuario = Objects.requireNonNull(usuario, "usuario nao deve ser nulo");
		if (tokenHash == null || tokenHash.isBlank()) {
			throw new IllegalArgumentException("tokenHash nao deve ser nulo ou em branco.");
		}
		this.tokenHash = tokenHash;
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt nao deve ser nulo");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt nao deve ser nulo");
		this.revoked = false;
	}

	public boolean isExpired(Instant now) {
		Objects.requireNonNull(now, "now nao deve ser nulo");
		return !this.expiresAt.isAfter(now);
	}

	public boolean isActive(Instant now) {
		return !this.revoked && !isExpired(now);
	}

	public void revoke(Instant now) {
		Objects.requireNonNull(now, "now nao deve ser nulo");
		if (!this.revoked) {
			this.revoked = true;
			this.revokedAt = now;
		}
	}

	public void markRotated(UUID replacementTokenId, Instant now) {
		Objects.requireNonNull(replacementTokenId, "replacementTokenId nao deve ser nulo");
		if (Objects.equals(this.id, replacementTokenId)) {
			throw new IllegalArgumentException("Refresh token nao pode referenciar a si mesmo como substituto.");
		}
		revoke(now);
		this.replacedByTokenId = replacementTokenId;
	}

	@PrePersist
	protected void onPrePersist() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RefreshToken other = (RefreshToken) o;
		return this.id != null && Objects.equals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "RefreshToken{"
				+ "id=" + id
				+ ", expiresAt=" + expiresAt
				+ ", revoked=" + revoked
				+ ", replacedByTokenId=" + replacedByTokenId
				+ '}';
	}
}
