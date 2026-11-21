package com.uniflow.usuario.enums;

/**
 * Estados possiveis da conta de usuario na plataforma UniFlow.
 */
public enum StatusUsuario {
	ATIVO,
	INATIVO,
	BLOQUEADO,
	EXCLUIDO;

	public boolean permiteAutenticacao() {
		return this == ATIVO;
	}
}
