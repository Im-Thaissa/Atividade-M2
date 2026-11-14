package com.pfc.thindesk.dto;

public record RankingItemResponse(int posicao, String nome, long total, long resolvidos, double percentual,
        Double tempoMedioResolucaoHoras) {
}
