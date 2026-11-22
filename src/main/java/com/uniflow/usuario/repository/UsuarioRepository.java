package com.uniflow.usuario.repository;

import com.uniflow.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para persistencia e consultas de {@link Usuario}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	/**
	 * Busca um usuario pelo e-mail ignorando diferencas de maiusculas e minusculas.
	 *
	 * @param email e-mail de busca
	 * @return {@link Optional} com o usuario encontrado, quando existir
	 */
	Optional<Usuario> findByEmailIgnoreCase(String email);

	/**
	 * Verifica se ja existe usuario cadastrado com o e-mail informado, ignorando maiusculas e minusculas.
	 *
	 * @param email e-mail a verificar
	 * @return {@code true} caso exista registro com o mesmo e-mail
	 */
	boolean existsByEmailIgnoreCase(String email);
}
