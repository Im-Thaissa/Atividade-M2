package com.pfc.thindesk;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

/** Testes de integração dos endpoints /api/chamados (RF06 e RF07). */
class ChamadoApiTest extends TesteBase {

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private String criar(String json) throws Exception {
        String resposta = mvc.perform(post("/api/chamados").with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    @Test
    @WithMockUser
    @DisplayName("POST cria chamado aplicando valores padrão e normalização")
    void criarChamado() throws Exception {
        mvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                        {"descricao": "  Impressora   não imprime ", "tipo": "Impressora", "usuario": "Beatriz",
                         "tecnico": "Rafael", "setor": "RH"}
                        """))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/chamados/")))
                .andExpect(jsonPath("$.descricao").value("Impressora não imprime"))
                .andExpect(jsonPath("$.status").value("Aberto"))
                .andExpect(jsonPath("$.prioridade").value("Média"))
                .andExpect(jsonPath("$.dataAbertura").value("2026-11-14T12:00:00"))
                .andExpect(jsonPath("$.dataFechamento").value(nullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST com status 'resolvido' normaliza e registra a data de fechamento")
    void criarResolvido() throws Exception {
        mvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                        {"descricao": "Senha expirada", "tipo": "Acesso/Senha", "usuario": "Caio",
                         "status": "resolvido", "prioridade": "ALTA", "dataAbertura": "2026-11-13T10:00:00"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Resolvido"))
                .andExpect(jsonPath("$.prioridade").value("Alta"))
                .andExpect(jsonPath("$.dataFechamento").value("2026-11-14T12:00:00"))
                .andExpect(jsonPath("$.tempoResolucaoHoras").value(26.0));
    }

    @Test
    @WithMockUser
    @DisplayName("POST inválido retorna 400 com a lista de campos")
    void criarInvalido() throws Exception {
        mvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                        {"descricao": "", "usuario": "Caio"}
                        """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos na requisição"))
                .andExpect(jsonPath("$.detalhes", hasSize(2)));

        mvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON).content("{ isso não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição ausente ou em formato JSON inválido"));

        mvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                        {"descricao": "x", "tipo": "Rede", "usuario": "Caio", "prioridade": "urgentíssima"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("prioridade inválida")));
    }

    @Test
    @WithMockUser
    @DisplayName("GET por id inexistente retorna 404")
    void buscarInexistente() throws Exception {
        mvc.perform(get("/api/chamados/000000000000000000000000"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Chamado não encontrado com id: 000000000000000000000000"));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH de status registra e limpa a data de fechamento (RF07)")
    void atualizarStatus() throws Exception {
        String id = criar("""
                {"descricao": "Wi-Fi caindo", "tipo": "Rede", "usuario": "Daniela"}
                """);

        mvc.perform(patch("/api/chamados/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("""
                        {"status": "Resolvido", "consideracoesTecnicas": "Canal do access point alterado."}
                        """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Resolvido"))
                .andExpect(jsonPath("$.consideracoesTecnicas").value("Canal do access point alterado."))
                .andExpect(jsonPath("$.dataFechamento").value("2026-11-14T12:00:00"));

        mvc.perform(patch("/api/chamados/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("""
                        {"status": "em andamento"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Em andamento"))
                .andExpect(jsonPath("$.dataFechamento").value(nullValue()));

        mvc.perform(patch("/api/chamados/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("""
                        {"status": "fechado"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT atualiza todos os campos do chamado")
    void atualizarCompleto() throws Exception {
        String id = criar("""
                {"descricao": "Monitor piscando", "tipo": "Hardware", "usuario": "Eduardo"}
                """);
        mvc.perform(put("/api/chamados/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"descricao": "Monitor piscando - trocar cabo", "tipo": "Hardware", "usuario": "Eduardo",
                         "tecnico": "Carlos", "prioridade": "baixa", "status": "Aguardando"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Monitor piscando - trocar cabo"))
                .andExpect(jsonPath("$.tecnico").value("Carlos"))
                .andExpect(jsonPath("$.status").value("Aguardando"))
                .andExpect(jsonPath("$.dataAbertura").value("2026-11-14T12:00:00"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET lista com filtros, paginação e ordenação")
    void listarPaginado() throws Exception {
        repository.saveAll(List.of(
                chamado("Rede", "João", "TI", "Alta", "Aberto", dia(11, 1, 9), null),
                chamado("Rede", "João", "TI", "Alta", "Aberto", dia(11, 2, 9), null),
                chamado("Rede", "Ana", "TI", "Alta", "Resolvido", dia(11, 3, 9), dia(11, 3, 10)),
                chamado("Hardware", "João", "TI", "Alta", "Aberto", dia(11, 4, 9), null)));

        mvc.perform(get("/api/chamados").param("tipo", "rede").param("tamanho", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.conteudo[0].dataAbertura").value("2026-11-03T09:00:00"));

        mvc.perform(get("/api/chamados").param("tamanho", "500"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/chamados").param("ordenarPor", "senha"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sem autenticação a API responde 401")
    void semAutenticacao() throws Exception {
        mvc.perform(get("/api/chamados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/indicadores")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Autenticação HTTP Basic com usuário configurado")
    void autenticacaoBasic() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.httpBasic("admin", "thindesk")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/indicadores").with(org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.httpBasic("admin", "errada")))
                .andExpect(status().isUnauthorized());
    }
}
