package com.pfc.thindesk.dto;

import java.util.List;

public record FiltrosDisponiveisResponse(List<String> status, List<String> prioridades, List<String> tipos,
        List<String> tecnicos, List<String> setores) {
}
