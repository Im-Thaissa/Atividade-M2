package com.pfc.thindesk.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroResponse(OffsetDateTime timestamp, int status, String erro, String mensagem, String caminho,
        List<String> detalhes) {
}
