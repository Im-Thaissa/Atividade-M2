package com.pfc.thindesk.exception;

/** Exceções de negócio da API. */
public final class Excecoes {

    private Excecoes() {
    }

    /** Recurso (ex.: chamado) não encontrado -> HTTP 404. */
    public static class RecursoNaoEncontradoException extends RuntimeException {
        public RecursoNaoEncontradoException(String mensagem) {
            super(mensagem);
        }
    }

    /** Parâmetro ou dado de entrada inválido segundo as regras de negócio -> HTTP 400. */
    public static class RequisicaoInvalidaException extends RuntimeException {
        public RequisicaoInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
