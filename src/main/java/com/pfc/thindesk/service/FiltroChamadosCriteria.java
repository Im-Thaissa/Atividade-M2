package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.FiltroChamados;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.Excecoes.RequisicaoInvalidaException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.query.Criteria;

public final class FiltroChamadosCriteria {

    private FiltroChamadosCriteria() {
    }

    public static Criteria montar(FiltroChamados filtro) {
        validar(filtro);
        List<Criteria> partes = new ArrayList<>();

        if (filtro.dataInicio() != null || filtro.dataFim() != null) {
            Criteria data = Criteria.where("dataAbertura");
            if (filtro.dataInicio() != null) {
                data = data.gte(filtro.dataInicio().atStartOfDay());
            }
            if (filtro.dataFim() != null) {
                data = data.lt(filtro.dataFim().plusDays(1).atStartOfDay());
            }
            partes.add(data);
        }
        if (temValor(filtro.status())) {
            partes.add(Criteria.where("status").is(statusNormalizado(filtro.status())));
        }
        if (temValor(filtro.prioridade())) {
            partes.add(Criteria.where("prioridade").is(prioridadeNormalizada(filtro.prioridade())));
        }
        adicionarIgualSemCaixa(partes, "tipo", filtro.tipo());
        adicionarIgualSemCaixa(partes, "tecnico", filtro.tecnico());
        adicionarIgualSemCaixa(partes, "setor", filtro.setor());

        return partes.isEmpty() ? new Criteria() : new Criteria().andOperator(partes.toArray(new Criteria[0]));
    }

    public static void validar(FiltroChamados filtro) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new RequisicaoInvalidaException("dataInicio (" + filtro.dataInicio()
                    + ") não pode ser posterior a dataFim (" + filtro.dataFim() + ")");
        }
        if (temValor(filtro.status())) {
            statusNormalizado(filtro.status());
        }
        if (temValor(filtro.prioridade())) {
            prioridadeNormalizada(filtro.prioridade());
        }
    }

    public static String statusNormalizado(String valor) {
        return StatusChamado.de(valor)
                .map(StatusChamado::getRotulo)
                .orElseThrow(() -> new RequisicaoInvalidaException("status inválido: \"" + valor
                        + "\". Valores aceitos: " + StatusChamado.rotulos()));
    }

    public static String prioridadeNormalizada(String valor) {
        return Prioridade.de(valor)
                .map(Prioridade::getRotulo)
                .orElseThrow(() -> new RequisicaoInvalidaException("prioridade inválida: \"" + valor
                        + "\". Valores aceitos: " + Prioridade.rotulos()));
    }

    private static void adicionarIgualSemCaixa(List<Criteria> partes, String campo, String valor) {
        if (temValor(valor)) {
            partes.add(Criteria.where(campo).regex("^" + Pattern.quote(valor.trim()) + "$", "i"));
        }
    }

    static boolean temValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
