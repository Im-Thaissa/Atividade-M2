package com.pfc.thindesk.dto;

import java.time.LocalDate;

public record PontoSerieResponse(LocalDate inicio, String rotulo, long abertos, long resolvidos, long cancelados) {
}
