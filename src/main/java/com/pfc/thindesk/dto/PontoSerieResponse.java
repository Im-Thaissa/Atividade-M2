package com.pfc.thindesk.dto;

import java.time.LocalDate;

/** Um ponto do gráfico de chamados por período (RF02). */
public record PontoSerieResponse(LocalDate inicio, String rotulo, long abertos, long resolvidos, long cancelados) {
}
