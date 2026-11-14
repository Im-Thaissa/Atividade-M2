package com.pfc.thindesk.entity;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Situações possíveis de um chamado. O valor gravado no banco é o "rotulo",
 * mantendo compatibilidade com os registros antigos (ex.: "Aberto").
 */
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

    /** Indica se o chamado não exige mais ação da equipe (Resolvido ou Cancelado). */
    public boolean isFinalizado() {
        return finalizado;
    }

    /** Rótulos dos status que ainda estão pendentes de atendimento. */
    public static List<String> rotulosPendentes() {
        return Arrays.stream(values()).filter(s -> !s.finalizado).map(StatusChamado::getRotulo).toList();
    }

    public static List<String> rotulos() {
        return Arrays.stream(values()).map(StatusChamado::getRotulo).toList();
    }

    /**
     * Converte um texto livre em status, ignorando maiúsculas, acentos, espaços e "_".
     * Aceita tanto o rótulo ("Em andamento") quanto o nome ("EM_ANDAMENTO").
     */
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
