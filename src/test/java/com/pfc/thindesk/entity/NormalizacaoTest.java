package com.pfc.thindesk.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NormalizacaoTest {

    @ParameterizedTest
    @ValueSource(strings = {"Em andamento", "em andamento", "EM_ANDAMENTO", "em-andamento", "  EmAndamento "})
    @DisplayName("Status aceita variações de escrita")
    void statusVariacoes(String valor) {
        assertThat(StatusChamado.de(valor)).contains(StatusChamado.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("Status desconhecido ou vazio não é aceito")
    void statusInvalido() {
        assertThat(StatusChamado.de("fechado")).isEmpty();
        assertThat(StatusChamado.de(" ")).isEmpty();
        assertThat(StatusChamado.de(null)).isEmpty();
    }

    @Test
    @DisplayName("Prioridade ignora acentos e caixa")
    void prioridade() {
        assertThat(Prioridade.de("critica")).contains(Prioridade.CRITICA);
        assertThat(Prioridade.de("MÉDIA")).contains(Prioridade.MEDIA);
        assertThat(Prioridade.CRITICA.getSlaHoras()).isEqualTo(4);
    }

    @Test
    @DisplayName("Somente Resolvido e Cancelado são finalizados")
    void finalizados() {
        assertThat(StatusChamado.rotulosPendentes()).containsExactly("Aberto", "Em andamento", "Aguardando");
    }
}
