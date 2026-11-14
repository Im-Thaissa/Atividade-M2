package com.pfc.thindesk.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

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

    public FiltroChamados semDatas() {
        return new FiltroChamados(null, null, status, tipo, tecnico, prioridade, setor);
    }
}
