package com.pfc.thindesk.dto;

import java.time.LocalDate;
import java.util.List;

public record SerieTemporalResponse(String agrupamento, LocalDate dataInicio, LocalDate dataFim,
        long totalAbertos, long totalResolvidos, List<PontoSerieResponse> pontos) {
}
