package com.pfc.thindesk.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chamados")
public class Chamado {

    @Id
    private String id;
    private String descricao;
    @Indexed
    private String status;
    @Indexed
    private String tipo;
    @Indexed
    private String tecnico;
    private String usuario;
    /** Setor do cliente que abriu o chamado. */
    private String setor;
    /** Rótulo da prioridade (Baixa, Média, Alta, Crítica). */
    private String prioridade;
    /** Considerações técnicas registradas pelo técnico durante/após o atendimento. */
    private String consideracoesTecnicas;
    @Indexed
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;

    /** Tempo de resolução em horas, ou null se o chamado ainda não foi fechado. */
    @org.springframework.data.annotation.Transient
    public Double getTempoResolucaoHoras() {
        if (dataAbertura == null || dataFechamento == null) {
            return null;
        }
        return Duration.between(dataAbertura, dataFechamento).toMinutes() / 60.0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getConsideracoesTecnicas() {
        return consideracoesTecnicas;
    }

    public void setConsideracoesTecnicas(String consideracoesTecnicas) {
        this.consideracoesTecnicas = consideracoesTecnicas;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }
}
