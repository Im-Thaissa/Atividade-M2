package com.pfc.thindesk.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de consulta aceitos pelos endpoints do dashboard e da listagem de chamados (RF05).
 * Todos os campos são opcionais. As datas filtram pela data de abertura do chamado (formato yyyy-MM-dd).
 */
public record FiltroChamados(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        String status,
        String tipo,
        String tecnico,
        String prioridade,
        String setor) {

    public static FiltroChamados vazio() {
        return new FiltroChamados(null, null, null, null, null, null, null);
    }

    /** Cópia do filtro sem o intervalo de datas (usado na série temporal, que trata o período à parte). */
    public FiltroChamados semDatas() {
        return new FiltroChamados(null, null, status, tipo, tecnico, prioridade, setor);
    }
}
