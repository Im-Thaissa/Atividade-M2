package com.pfc.thindesk.dto;

import java.time.LocalDateTime;

/** Indicadores gerais exibidos nos cards do dashboard (RF01). */
public record IndicadoresResponse(
        long total,
        long pendentes,
        long abertos,
        long emAndamento,
        long aguardando,
        long resolvidos,
        long cancelados,
        long criticosPendentes,
        long pendentesForaDoSla,
        long abertosHoje,
        double taxaResolucao,
        Double tempoMedioResolucaoHoras,
        Double percentualResolvidosNoSla,
        LocalDateTime atualizadoEm) {
}
