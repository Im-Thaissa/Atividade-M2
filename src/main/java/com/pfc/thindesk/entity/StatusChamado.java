package com.pfc.thindesk.entity;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum StatusChamado {

    ABERTO("Aberto", false),
    EM_ANDAMENTO("Em andamento", false),
    AGUARDANDO("Aguardando", false),
    RESOLVIDO("Resolvido", true),
    CANCELADO("Cancelado", true);

    private final String rotulo;
    private final boolean finalizado;

    StatusChamado(String rotulo, boolean finalizado) {
        this.rotulo = rotulo;
        this.finalizado = finalizado;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public static List<String> rotulosPendentes() {
        return Arrays.stream(values()).filter(s -> !s.finalizado).map(StatusChamado::getRotulo).toList();
    }

    public static List<String> rotulos() {
        return Arrays.stream(values()).map(StatusChamado::getRotulo).toList();
    }

    public static Optional<StatusChamado> de(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String chave = normalizar(valor);
        return Arrays.stream(values())
                .filter(s -> normalizar(s.rotulo).equals(chave) || normalizar(s.name()).equals(chave))
                .findFirst();
    }

    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("[\\s_-]+", "");
    }
}
