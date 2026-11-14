package com.pfc.thindesk.dto;

/** Quantidade de chamados em uma situação (RF03). */
public record ContagemStatusResponse(String status, long quantidade, double percentual, boolean finalizado) {
}
