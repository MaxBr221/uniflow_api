package com.uniflow.auth.security;

import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ResourceNotFoundException;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Componente central de validacao de propriedade (ownership) e acesso seguro por usuario autenticado ({@code BE-030}).
 * <p>
 * Garante que identificadores de usuario sejam lidos exclusivamente do {@code SecurityContext} via
 * {@link CurrentUserService}, bloqueando com {@code 403 FORBIDDEN_OPERATION} qualquer tentativa de
 * acesso ou mutacao de recurso pertencente a outro usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnershipValidator {

	private static final String DEFAULT_DENIED_MESSAGE = "Voce nao possui permissao para acessar ou alterar este recurso.";
	private static final String DEFAULT_NOT_FOUND_MESSAGE = "Recurso solicitado nao encontrado.";

	private final CurrentUserService currentUserService;

	public UUID getCurrentUserId() {
		return currentUserService.getCurrentUserId();
	}

	public Usuario getCurrentUser() {
		return currentUserService.getCurrentUser();
	}

	public void assertCurrentUserIsOwner(UUID ownerUserId) {
		assertCurrentUserIsOwner(ownerUserId, DEFAULT_DENIED_MESSAGE);
	}

	public void assertCurrentUserIsOwner(UUID ownerUserId, String deniedMessage) {
		UUID currentUserId = currentUserService.getCurrentUserId();
		assertOwner(ownerUserId, currentUserId, deniedMessage);
	}

	public <T> T requireOwned(
			T resource,
			Function<T, UUID> ownerIdExtractor,
			String notFoundMessage,
			String deniedMessage) {
		Objects.requireNonNull(ownerIdExtractor, "ownerIdExtractor nao deve ser nulo");
		UUID currentUserId = currentUserService.getCurrentUserId();
		if (resource == null) {
			throw new ResourceNotFoundException(resolveMessage(notFoundMessage, DEFAULT_NOT_FOUND_MESSAGE));
		}
		assertOwner(ownerIdExtractor.apply(resource), currentUserId, deniedMessage);
		return resource;
	}

	public <T> T requireOwned(
			Optional<T> resourceOpt,
			Function<T, UUID> ownerIdExtractor,
			String notFoundMessage,
			String deniedMessage) {
		Objects.requireNonNull(resourceOpt, "resourceOpt nao deve ser nulo");
		return requireOwned(resourceOpt.orElse(null), ownerIdExtractor, notFoundMessage, deniedMessage);
	}

	public static void assertOwner(UUID ownerUserId, UUID authenticatedUserId, String deniedMessage) {
		if (authenticatedUserId == null) {
			throw new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas.");
		}
		if (ownerUserId == null || !ownerUserId.equals(authenticatedUserId)) {
			log.warn("Tentativa de acesso/alteracao de recurso de outro usuario bloqueada (actorUsuarioId={})", authenticatedUserId);
			throw new UnauthorizedOperationException(resolveMessage(deniedMessage, DEFAULT_DENIED_MESSAGE));
		}
	}

	private static String resolveMessage(String message, String fallback) {
		return (message != null && !message.isBlank()) ? message : fallback;
	}
}
