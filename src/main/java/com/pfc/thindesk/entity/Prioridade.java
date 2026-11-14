package com.pfc.thindesk.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Prioridade {

    BAIXA("Baixa", 72),
    MEDIA("Média", 24),
    ALTA("Alta", 8),
    CRITICA("Crítica", 4);

    private final String rotulo;
    private final int slaHoras;

    Prioridade(String rotulo, int slaHoras) {
        this.rotulo = rotulo;
        this.slaHoras = slaHoras;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getSlaHoras() {
        return slaHoras;
    }

    public static List<String> rotulos() {
        return Arrays.stream(values()).map(Prioridade::getRotulo).toList();
    }

    public static Optional<Prioridade> de(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String chave = StatusChamado.normalizar(valor);
        return Arrays.stream(values())
                .filter(p -> StatusChamado.normalizar(p.rotulo).equals(chave) || StatusChamado.normalizar(p.name()).equals(chave))
                .findFirst();
    }
}
