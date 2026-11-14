package com.pfc.thindesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoStatusRequest(
        @NotBlank(message = "status é obrigatório") String status,
        @Size(max = 2000, message = "consideracoesTecnicas deve ter no máximo 2000 caracteres") String consideracoesTecnicas) {
}
