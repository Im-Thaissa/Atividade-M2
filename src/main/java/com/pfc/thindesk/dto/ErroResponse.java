package com.pfc.thindesk.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Formato padrão de erro devolvido pela API. */
public record ErroResponse(OffsetDateTime timestamp, int status, String erro, String mensagem, String caminho,
        List<String> detalhes) {
}
