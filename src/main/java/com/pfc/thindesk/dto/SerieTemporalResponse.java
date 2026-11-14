package com.pfc.thindesk.dto;

import java.time.LocalDate;
import java.util.List;

/** Série temporal de chamados abertos x resolvidos (RF02). */
public record SerieTemporalResponse(String agrupamento, LocalDate dataInicio, LocalDate dataFim,
        long totalAbertos, long totalResolvidos, List<PontoSerieResponse> pontos) {
}
