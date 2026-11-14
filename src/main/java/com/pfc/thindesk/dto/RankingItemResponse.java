package com.pfc.thindesk.dto;

/** Item de ranking (tipos de chamado ou técnicos) - RF04. */
public record RankingItemResponse(int posicao, String nome, long total, long resolvidos, double percentual,
        Double tempoMedioResolucaoHoras) {
}
