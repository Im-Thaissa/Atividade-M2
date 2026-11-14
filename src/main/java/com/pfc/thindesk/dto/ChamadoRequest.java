package com.pfc.thindesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ChamadoRequest(
        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 500, message = "descricao deve ter no máximo 500 caracteres") String descricao,
        @NotBlank(message = "tipo é obrigatório") String tipo,
        @NotBlank(message = "usuario é obrigatório") String usuario,
        String tecnico,
        String setor,
        String prioridade,
        String status,
        @Size(max = 2000, message = "consideracoesTecnicas deve ter no máximo 2000 caracteres") String consideracoesTecnicas,
        @PastOrPresent(message = "dataAbertura não pode estar no futuro") LocalDateTime dataAbertura) {
}
