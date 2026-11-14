package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.ChamadoRequest;
import com.pfc.thindesk.dto.FiltroChamados;
import com.pfc.thindesk.dto.PaginaResponse;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.Excecoes.RecursoNaoEncontradoException;
import com.pfc.thindesk.exception.Excecoes.RequisicaoInvalidaException;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("dataAbertura", "dataFechamento", "status", "tipo",
            "tecnico", "prioridade", "setor");

    private final ChamadoRepository chamadoRepository;
    private final MongoTemplate mongoTemplate;
    private final Clock relogio;

    public ChamadoService(ChamadoRepository chamadoRepository, MongoTemplate mongoTemplate, Clock relogio) {
        this.chamadoRepository = chamadoRepository;
        this.mongoTemplate = mongoTemplate;
        this.relogio = relogio;
    }

    /** Lista todos os chamados (usado pela tela Thymeleaf existente). */
    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataAbertura"));
    }

    /** Consulta paginada e filtrada de chamados. */
    public PaginaResponse<Chamado> pesquisar(FiltroChamados filtro, int pagina, int tamanho, String ordenarPor, String direcao) {
        if (pagina < 0) {
            throw new RequisicaoInvalidaException("pagina deve ser maior ou igual a 0");
        }
        if (tamanho < 1 || tamanho > 100) {
            throw new RequisicaoInvalidaException("tamanho deve estar entre 1 e 100");
        }
        if (!CAMPOS_ORDENACAO.contains(ordenarPor)) {
            throw new RequisicaoInvalidaException("ordenarPor inválido: \"" + ordenarPor + "\". Valores aceitos: "
                    + CAMPOS_ORDENACAO.stream().sorted().toList());
        }
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direcao);
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("direcao deve ser 'asc' ou 'desc'");
        }

        Query query = new Query(FiltroChamadosCriteria.montar(filtro));
        long total = mongoTemplate.count(query, Chamado.class);
        query.with(Sort.by(dir, ordenarPor)).skip((long) pagina * tamanho).limit(tamanho);
        List<Chamado> conteudo = mongoTemplate.find(query, Chamado.class);
        int totalPaginas = (int) Math.ceil(total / (double) tamanho);
        return new PaginaResponse<>(conteudo, pagina, tamanho, total, totalPaginas);
    }

    public Chamado buscarPorId(String id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado com id: " + id));
    }

    public Chamado criar(ChamadoRequest req) {
        Chamado chamado = new Chamado();
        aplicar(chamado, req);
        if (chamado.getDataAbertura() == null) {
            chamado.setDataAbertura(agora());
        }
        if (chamado.getStatus() == null) {
            chamado.setStatus(StatusChamado.ABERTO.getRotulo());
        }
        if (chamado.getPrioridade() == null) {
            chamado.setPrioridade(Prioridade.MEDIA.getRotulo());
        }
        ajustarFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    public Chamado atualizar(String id, ChamadoRequest req) {
        Chamado chamado = buscarPorId(id);
        LocalDateTime aberturaOriginal = chamado.getDataAbertura();
        aplicar(chamado, req);
        if (chamado.getDataAbertura() == null) {
            chamado.setDataAbertura(aberturaOriginal != null ? aberturaOriginal : agora());
        }
        ajustarFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    /** Altera apenas a situação (e opcionalmente as considerações técnicas) de um chamado. */
    public Chamado atualizarStatus(String id, String status, String consideracoesTecnicas) {
        Chamado chamado = buscarPorId(id);
        chamado.setStatus(FiltroChamadosCriteria.statusNormalizado(status));
        if (consideracoesTecnicas != null && !consideracoesTecnicas.isBlank()) {
            chamado.setConsideracoesTecnicas(consideracoesTecnicas.trim());
        }
        ajustarFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    /** Atualização vinda do formulário Thymeleaf legado. */
    public Chamado atualizarChamado(String id, Chamado dados) {
        Chamado chamado = buscarPorId(id);
        chamado.setDescricao(dados.getDescricao());
        chamado.setTipo(dados.getTipo());
        chamado.setTecnico(dados.getTecnico());
        chamado.setUsuario(dados.getUsuario());
        if (dados.getStatus() != null && !dados.getStatus().isBlank()) {
            chamado.setStatus(FiltroChamadosCriteria.statusNormalizado(dados.getStatus()));
        }
        ajustarFechamento(chamado);
        return chamadoRepository.save(chamado);
    }

    /** Normaliza e copia os dados da requisição para a entidade. */
    private void aplicar(Chamado chamado, ChamadoRequest req) {
        chamado.setDescricao(limpar(req.descricao()));
        chamado.setTipo(limpar(req.tipo()));
        chamado.setUsuario(limpar(req.usuario()));
        chamado.setTecnico(limpar(req.tecnico()));
        chamado.setSetor(limpar(req.setor()));
        chamado.setConsideracoesTecnicas(limpar(req.consideracoesTecnicas()));
        if (req.status() != null && !req.status().isBlank()) {
            chamado.setStatus(FiltroChamadosCriteria.statusNormalizado(req.status()));
        }
        if (req.prioridade() != null && !req.prioridade().isBlank()) {
            chamado.setPrioridade(FiltroChamadosCriteria.prioridadeNormalizada(req.prioridade()));
        }
        if (req.dataAbertura() != null) {
            chamado.setDataAbertura(req.dataAbertura());
        }
    }

    /**
     * Regra de negócio: chamados finalizados (Resolvido/Cancelado) recebem data de fechamento;
     * ao serem reabertos, a data de fechamento é removida.
     */
    private void ajustarFechamento(Chamado chamado) {
        boolean finalizado = StatusChamado.de(chamado.getStatus()).map(StatusChamado::isFinalizado).orElse(false);
        if (finalizado && chamado.getDataFechamento() == null) {
            chamado.setDataFechamento(agora());
        } else if (!finalizado) {
            chamado.setDataFechamento(null);
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim().replaceAll("\\s+", " ");
        return v.isEmpty() ? null : v;
    }
}
