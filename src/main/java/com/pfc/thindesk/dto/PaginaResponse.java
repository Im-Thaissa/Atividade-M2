package com.pfc.thindesk.dto;

import java.util.List;

/** Envelope de resposta paginada. */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
}
