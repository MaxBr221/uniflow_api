package com.uniflow.auth.security;

import com.uniflow.common.exception.AuthenticationFailedException;
import com.uniflow.common.exception.ResourceNotFoundException;
import com.uniflow.common.exception.UnauthorizedOperationException;
import com.uniflow.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipValidatorTest {

	@Mock
	private CurrentUserService currentUserService;

	private OwnershipValidator ownershipValidator;

	@BeforeEach
	void setUp() {
		ownershipValidator = new OwnershipValidator(currentUserService);
	}

	@Test
	void shouldDelegateCurrentUserLookupsToCurrentUserService() {
		UUID userId = UUID.randomUUID();
		Usuario usuario = new Usuario("Ana Silva", "ana@uniflow.edu", "$2a$10$hashSecreto");
		usuario.setId(userId);

		when(currentUserService.getCurrentUserId()).thenReturn(userId);
		when(currentUserService.getCurrentUser()).thenReturn(usuario);

		assertThat(ownershipValidator.getCurrentUserId()).isEqualTo(userId);
		assertThat(ownershipValidator.getCurrentUser()).isSameAs(usuario);
		verify(currentUserService).getCurrentUserId();
		verify(currentUserService).getCurrentUser();
	}

	@Test
	void shouldAllowOwnedResourceAndRejectForeignNullOwnerOrUnauthenticatedAccess() {
		UUID ownerId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		DummyOwnedResource owned = new DummyOwnedResource(ownerId, "Meu Semestre");

		when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
		assertThatCode(() -> ownershipValidator.assertCurrentUserIsOwner(ownerId)).doesNotThrowAnyException();
		assertThat(ownershipValidator.requireOwned(owned, DummyOwnedResource::ownerId, "Nao encontrado", "Negado"))
				.isSameAs(owned);
		assertThat(ownershipValidator.requireOwned(Optional.of(owned), DummyOwnedResource::ownerId, "Nao encontrado", "Negado"))
				.isSameAs(owned);

		assertThatThrownBy(() -> ownershipValidator.assertCurrentUserIsOwner(otherId))
				.isInstanceOf(UnauthorizedOperationException.class)
				.hasMessage("Voce nao possui permissao para acessar ou alterar este recurso.");

		assertThatThrownBy(() -> ownershipValidator.assertCurrentUserIsOwner(null, "Acesso negado customizado"))
				.isInstanceOf(UnauthorizedOperationException.class)
				.hasMessage("Acesso negado customizado");

		assertThatThrownBy(() -> ownershipValidator.requireOwned(
				Optional.of(new DummyOwnedResource(otherId, "Alheio")),
				DummyOwnedResource::ownerId,
				"Semestre nao encontrado.",
				"Sem permissao."
		))
				.isInstanceOf(UnauthorizedOperationException.class)
				.hasMessage("Sem permissao.");

		assertThatThrownBy(() -> ownershipValidator.requireOwned(
				Optional.<DummyOwnedResource>empty(),
				DummyOwnedResource::ownerId,
				"Semestre nao encontrado.",
				"Sem permissao."
		))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Semestre nao encontrado.");

		when(currentUserService.getCurrentUserId())
				.thenThrow(new AuthenticationFailedException("Autenticacao necessaria ou credenciais invalidas."));

		assertThatThrownBy(() -> ownershipValidator.requireOwned(
				(DummyOwnedResource) null,
				DummyOwnedResource::ownerId,
				"Semestre nao encontrado.",
				"Sem permissao."
		)).isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void shouldEnforceStaticAssertOwnerInvariants() {
		UUID userId = UUID.randomUUID();
		UUID foreignId = UUID.randomUUID();

		assertThatCode(() -> OwnershipValidator.assertOwner(userId, userId, null)).doesNotThrowAnyException();
		assertThatThrownBy(() -> OwnershipValidator.assertOwner(userId, null, "Negado"))
				.isInstanceOf(AuthenticationFailedException.class);
		assertThatThrownBy(() -> OwnershipValidator.assertOwner(null, userId, " "))
				.isInstanceOf(UnauthorizedOperationException.class)
				.hasMessage("Voce nao possui permissao para acessar ou alterar este recurso.");
		assertThatThrownBy(() -> OwnershipValidator.assertOwner(foreignId, userId, "Recurso protegido"))
				.isInstanceOf(UnauthorizedOperationException.class)
				.hasMessage("Recurso protegido");
	}

	private record DummyOwnedResource(UUID ownerId, String title) {
	}
}
