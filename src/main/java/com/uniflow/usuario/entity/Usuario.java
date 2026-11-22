package com.uniflow.usuario.entity;

import com.uniflow.usuario.enums.StatusUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA que representa uma conta de usuario na plataforma UniFlow.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
		name = "usuarios",
		uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email")
)
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private StatusUsuario status = StatusUsuario.ATIVO;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Usuario(String nome, String email, String senhaHash) {
		this.nome = normalizeNome(nome);
		this.email = normalizeEmail(email);
		this.senhaHash = senhaHash;
		this.status = StatusUsuario.ATIVO;
	}

	public void setNome(String nome) {
		this.nome = normalizeNome(nome);
	}

	public void setEmail(String email) {
		this.email = normalizeEmail(email);
	}

	public boolean podeAutenticar() {
		return this.status != null && this.status.permiteAutenticacao();
	}

	@PrePersist
	protected void onPrePersist() {
		this.nome = normalizeNome(this.nome);
		this.email = normalizeEmail(this.email);
		if (this.status == null) {
			this.status = StatusUsuario.ATIVO;
		}
		Instant now = Instant.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		this.updatedAt = now;
	}

	@PreUpdate
	protected void onPreUpdate() {
		this.nome = normalizeNome(this.nome);
		this.email = normalizeEmail(this.email);
		this.updatedAt = Instant.now();
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeNome(String nome) {
		return nome == null ? null : nome.trim();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Usuario other = (Usuario) o;
		return this.id != null && Objects.equals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "Usuario{"
				+ "id=" + id
				+ ", email='" + email + '\''
				+ ", status=" + status
				+ '}';
	}
}
