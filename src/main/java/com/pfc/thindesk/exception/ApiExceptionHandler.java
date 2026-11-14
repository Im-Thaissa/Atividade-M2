package com.pfc.thindesk.exception;

import com.pfc.thindesk.dto.ErroResponse;
import com.pfc.thindesk.exception.Excecoes.RecursoNaoEncontradoException;
import com.pfc.thindesk.exception.Excecoes.RequisicaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte as exceções da API em respostas JSON padronizadas ({@link ErroResponse}).
 */
@RestControllerAdvice(basePackages = "com.pfc.thindesk.controller.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> invalida(RequisicaoInvalidaException e, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage(), req, List.of());
    }

    /** Erros de validação do corpo (@Valid) e de conversão dos parâmetros de filtro (ex.: data inválida). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroResponse> validacao(BindException e, HttpServletRequest req) {
        List<String> detalhes = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + (f.isBindingFailure()
                        ? "valor inválido \"" + f.getRejectedValue() + "\""
                        : f.getDefaultMessage()))
                .sorted()
                .toList();
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos na requisição", req, detalhes);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro \"" + e.getName() + "\" com valor inválido: " + e.getValue(), req, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException e, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName(), req, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou em formato JSON inválido", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " não suportado neste endpoint", req, List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroResponse> banco(DataAccessException e, HttpServletRequest req) {
        log.error("Falha de acesso ao banco de dados", e);
        return erro(HttpStatus.SERVICE_UNAVAILABLE, "Banco de dados indisponível no momento. Tente novamente.", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado na API", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição", req, List.of());
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem, HttpServletRequest req, List<String> detalhes) {
        ErroResponse corpo = new ErroResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI(), detalhes);
        return ResponseEntity.status(status).body(corpo);
    }
}
