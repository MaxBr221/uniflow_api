package com.uniflow.common.exception;

/**
 * Representa um erro de validacao ou preenchimento associado a um campo especifico da requisicao.
 *
 * @param field   nome do campo ou parametro rejeitado
 * @param message mensagem descritiva da violacao encontrada
 */
public record FieldErrorDetail(
		String field,
		String message
) {
}
