package com.pfc.thindesk.exception;

public final class Excecoes {

    private Excecoes() {
    }

    public static class RecursoNaoEncontradoException extends RuntimeException {
        public RecursoNaoEncontradoException(String mensagem) {
            super(mensagem);
        }
    }

    public static class RequisicaoInvalidaException extends RuntimeException {
        public RequisicaoInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
