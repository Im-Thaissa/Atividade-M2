package com.pfc.thindesk.dto;

import java.util.List;

/** Valores disponíveis para popular os campos de filtro do dashboard (RF05). */
public record FiltrosDisponiveisResponse(List<String> status, List<String> prioridades, List<String> tipos,
        List<String> tecnicos, List<String> setores) {
}
