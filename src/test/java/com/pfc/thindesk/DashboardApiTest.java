package com.pfc.thindesk;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.entity.Chamado;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser
class DashboardApiTest extends TesteBase {

    @BeforeEach
    void massaDeDados() {
        repository.deleteAll();
        Chamado legado = new Chamado();
        legado.setDescricao("Chamado de Teste");
        legado.setStatus("aberto");
        legado.setUsuario("Alberto");
        repository.saveAll(List.of(
                chamado("Hardware", "João", "TI", "Alta", "Resolvido", dia(11, 10, 9), dia(11, 10, 13)),
                chamado("Hardware", "Mariana", "RH", "Média", "Resolvido", dia(11, 11, 8), dia(11, 12, 14)),
                chamado("Software", "João", "TI", "Baixa", "Aberto", dia(11, 14, 9), null),
                chamado("Rede", "Carlos", "Financeiro", "Crítica", "Em andamento", dia(11, 13, 8), null),
                chamado("Software", "Mariana", "RH", "Média", "Cancelado", dia(11, 1, 10), dia(11, 1, 11)),
                chamado("Hardware", "João", "TI", "Alta", "Aguardando", dia(10, 20, 10), null),
                legado));
    }

    @Test
    @DisplayName("RF01 - indicadores gerais sem filtro")
    void indicadoresGerais() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.pendentes").value(4))
                .andExpect(jsonPath("$.abertos").value(2))
                .andExpect(jsonPath("$.emAndamento").value(1))
                .andExpect(jsonPath("$.aguardando").value(1))
                .andExpect(jsonPath("$.resolvidos").value(2))
                .andExpect(jsonPath("$.cancelados").value(1))
                .andExpect(jsonPath("$.criticosPendentes").value(1))
                .andExpect(jsonPath("$.pendentesForaDoSla").value(2))
                .andExpect(jsonPath("$.abertosHoje").value(1))
                .andExpect(jsonPath("$.taxaResolucao").value(33.3))
                .andExpect(jsonPath("$.tempoMedioResolucaoHoras").value(17.0))
                .andExpect(jsonPath("$.percentualResolvidosNoSla").value(50.0));
    }

    @Test
    @DisplayName("RF05 - indicadores filtrados por técnico e por período")
    void indicadoresComFiltros() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").param("tecnico", "joão"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.resolvidos").value(1));

        mvc.perform(get("/api/dashboard/indicadores").param("dataInicio", "2026-11-10").param("dataFim", "2026-11-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resolvidos").value(2));

        mvc.perform(get("/api/dashboard/indicadores").param("status", "em_andamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("RF02 - série diária de abertos x resolvidos")
    void seriePorDia() throws Exception {
        mvc.perform(get("/api/dashboard/chamados-por-periodo")
                        .param("agrupamento", "dia").param("dataInicio", "2026-11-10").param("dataFim", "2026-11-14"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos", hasSize(5)))
                .andExpect(jsonPath("$.totalAbertos").value(4))
                .andExpect(jsonPath("$.totalResolvidos").value(2))
                .andExpect(jsonPath("$.pontos[0].inicio").value("2026-11-10"))
                .andExpect(jsonPath("$.pontos[0].abertos").value(1))
                .andExpect(jsonPath("$.pontos[0].resolvidos").value(1))
                .andExpect(jsonPath("$.pontos[2].resolvidos").value(1));
    }

    @Test
    @DisplayName("RF02 - agrupamento semanal com período padrão (12 semanas)")
    void seriePorSemanaPadrao() throws Exception {
        mvc.perform(get("/api/dashboard/chamados-por-periodo").param("agrupamento", "semana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agrupamento").value("semana"))
                .andExpect(jsonPath("$.pontos", hasSize(12)))
                .andExpect(jsonPath("$.totalAbertos").value(6));
    }

    @Test
    @DisplayName("RF03 - distribuição por situação, normalizando registros legados")
    void distribuicaoStatus() throws Exception {
        mvc.perform(get("/api/dashboard/status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].status").value("Aberto"))
                .andExpect(jsonPath("$[0].quantidade").value(2))
                .andExpect(jsonPath("$[0].percentual").value(28.6))
                .andExpect(jsonPath("$[3].status").value("Resolvido"))
                .andExpect(jsonPath("$[3].finalizado").value(true));
    }

    @Test
    @DisplayName("RF04 - ranking de tipos mais atendidos")
    void rankingTipos() throws Exception {
        mvc.perform(get("/api/dashboard/tipos").param("limite", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].posicao").value(1))
                .andExpect(jsonPath("$[0].nome").value("Hardware"))
                .andExpect(jsonPath("$[0].total").value(3))
                .andExpect(jsonPath("$[0].resolvidos").value(2))
                .andExpect(jsonPath("$[0].tempoMedioResolucaoHoras").value(17.0))
                .andExpect(jsonPath("$[1].nome").value("Software"))
                .andExpect(jsonPath("$[*].nome", hasItem("Não informado")));
    }

    @Test
    @DisplayName("Ranking de técnicos respeita o limite")
    void rankingTecnicos() throws Exception {
        mvc.perform(get("/api/dashboard/tecnicos").param("limite", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("João"))
                .andExpect(jsonPath("$[0].total").value(3));
    }

    @Test
    @DisplayName("RF05 - valores disponíveis para os filtros")
    void filtrosDisponiveis() throws Exception {
        mvc.perform(get("/api/dashboard/filtros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipos", hasSize(3)))
                .andExpect(jsonPath("$.tecnicos[0]").value("Carlos"))
                .andExpect(jsonPath("$.status", hasSize(5)))
                .andExpect(jsonPath("$.prioridades", hasItem("Crítica")));
    }

    @Test
    @DisplayName("Erro 400 - dataInicio posterior a dataFim")
    void erroIntervaloInvertido() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").param("dataInicio", "2026-11-20").param("dataFim", "2026-11-01"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem", containsString("não pode ser posterior")))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/indicadores"));
    }

    @Test
    @DisplayName("Erro 400 - data em formato inválido")
    void erroDataInvalida() throws Exception {
        mvc.perform(get("/api/dashboard/status").param("dataInicio", "14/11/2026"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("dataInicio")));
    }

    @Test
    @DisplayName("Erro 400 - status e agrupamento inválidos")
    void erroValoresInvalidos() throws Exception {
        mvc.perform(get("/api/dashboard/status").param("status", "finalizado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Valores aceitos")));

        mvc.perform(get("/api/dashboard/chamados-por-periodo").param("agrupamento", "ano"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("agrupamento inválido")));

        mvc.perform(get("/api/dashboard/tipos").param("limite", "0"))
                .andExpect(status().isBadRequest());
    }
}
