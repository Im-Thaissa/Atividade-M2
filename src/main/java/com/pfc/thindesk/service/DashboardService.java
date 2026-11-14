package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.ContagemStatusResponse;
import com.pfc.thindesk.dto.FiltroChamados;
import com.pfc.thindesk.dto.FiltrosDisponiveisResponse;
import com.pfc.thindesk.dto.IndicadoresResponse;
import com.pfc.thindesk.dto.PontoSerieResponse;
import com.pfc.thindesk.dto.RankingItemResponse;
import com.pfc.thindesk.dto.SerieTemporalResponse;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.Excecoes.RequisicaoInvalidaException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Regras de negócio e consultas que alimentam o Dashboard Administrativo.
 * Agrupamentos (status, tipos, técnicos) são feitos no próprio MongoDB via Aggregation Framework;
 * indicadores e série temporal são calculados a partir da consulta filtrada.
 */
@Service
public class DashboardService {

    static final String COLECAO = "chamados";
    static final String NAO_INFORMADO = "Não informado";
    private static final int MAX_PONTOS_SERIE = 400;
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMM/yy", Locale.forLanguageTag("pt-BR"));

    private final MongoTemplate mongoTemplate;
    private final Clock relogio;

    public DashboardService(MongoTemplate mongoTemplate, Clock relogio) {
        this.mongoTemplate = mongoTemplate;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------ RF01

    public IndicadoresResponse indicadores(FiltroChamados filtro) {
        List<Chamado> chamados = mongoTemplate.find(new Query(FiltroChamadosCriteria.montar(filtro)), Chamado.class, COLECAO);
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        Map<String, Long> porStatus = new LinkedHashMap<>();
        long criticosPendentes = 0;
        long pendentesForaSla = 0;
        long abertosHoje = 0;
        double somaHoras = 0;
        long qtdComTempo = 0;
        long resolvidosNoSla = 0;

        for (Chamado c : chamados) {
            StatusChamado status = StatusChamado.de(c.getStatus()).orElse(null);
            String chave = status == null ? NAO_INFORMADO : status.getRotulo();
            porStatus.merge(chave, 1L, Long::sum);

            Prioridade prioridade = Prioridade.de(c.getPrioridade()).orElse(Prioridade.MEDIA);
            boolean pendente = status != null && !status.isFinalizado();

            if (pendente && prioridade == Prioridade.CRITICA) {
                criticosPendentes++;
            }
            if (pendente && c.getDataAbertura() != null
                    && Duration.between(c.getDataAbertura(), agora).toHours() >= prioridade.getSlaHoras()) {
                pendentesForaSla++;
            }
            if (c.getDataAbertura() != null && c.getDataAbertura().toLocalDate().equals(hoje)) {
                abertosHoje++;
            }
            Double horas = c.getTempoResolucaoHoras();
            if (status == StatusChamado.RESOLVIDO && horas != null && horas >= 0) {
                somaHoras += horas;
                qtdComTempo++;
                if (horas <= prioridade.getSlaHoras()) {
                    resolvidosNoSla++;
                }
            }
        }

        long total = chamados.size();
        long abertos = porStatus.getOrDefault(StatusChamado.ABERTO.getRotulo(), 0L);
        long emAndamento = porStatus.getOrDefault(StatusChamado.EM_ANDAMENTO.getRotulo(), 0L);
        long aguardando = porStatus.getOrDefault(StatusChamado.AGUARDANDO.getRotulo(), 0L);
        long resolvidos = porStatus.getOrDefault(StatusChamado.RESOLVIDO.getRotulo(), 0L);
        long cancelados = porStatus.getOrDefault(StatusChamado.CANCELADO.getRotulo(), 0L);
        long validos = total - cancelados;

        return new IndicadoresResponse(
                total,
                abertos + emAndamento + aguardando,
                abertos, emAndamento, aguardando, resolvidos, cancelados,
                criticosPendentes, pendentesForaSla, abertosHoje,
                percentual(resolvidos, validos),
                qtdComTempo == 0 ? null : arredondar(somaHoras / qtdComTempo),
                qtdComTempo == 0 ? null : percentual(resolvidosNoSla, qtdComTempo),
                agora.truncatedTo(ChronoUnit.SECONDS));
    }

    // ------------------------------------------------------------------ RF02

    public SerieTemporalResponse seriePorPeriodo(FiltroChamados filtro, String agrupamento) {
        FiltroChamadosCriteria.validar(filtro);
        Agrupamento tipo = Agrupamento.de(agrupamento);
        LocalDate fim = filtro.dataFim() != null ? filtro.dataFim() : LocalDate.now(relogio);
        LocalDate inicio = filtro.dataInicio() != null ? filtro.dataInicio() : tipo.inicioPadrao(fim);

        LocalDate primeiroBalde = tipo.inicioDoBalde(inicio);
        long pontos = tipo.quantidadeDeBaldes(primeiroBalde, fim);
        if (pontos > MAX_PONTOS_SERIE) {
            throw new RequisicaoInvalidaException("Intervalo muito grande para agrupamento '" + tipo.nome
                    + "' (" + pontos + " pontos). Use um agrupamento maior ou reduza o período.");
        }

        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.plusDays(1).atStartOfDay();
        Criteria noPeriodo = new Criteria().orOperator(
                Criteria.where("dataAbertura").gte(de).lt(ate),
                Criteria.where("dataFechamento").gte(de).lt(ate));
        Criteria criterio = new Criteria().andOperator(FiltroChamadosCriteria.montar(filtro.semDatas()), noPeriodo);
        List<Chamado> chamados = mongoTemplate.find(new Query(criterio), Chamado.class, COLECAO);

        Map<LocalDate, long[]> baldes = new LinkedHashMap<>();
        for (LocalDate d = primeiroBalde; !d.isAfter(fim); d = tipo.proximo(d)) {
            baldes.put(d, new long[3]);
        }
        for (Chamado c : chamados) {
            if (dentro(c.getDataAbertura(), de, ate)) {
                baldes.get(tipo.inicioDoBalde(c.getDataAbertura().toLocalDate()))[0]++;
            }
            if (dentro(c.getDataFechamento(), de, ate)) {
                StatusChamado st = StatusChamado.de(c.getStatus()).orElse(null);
                long[] b = baldes.get(tipo.inicioDoBalde(c.getDataFechamento().toLocalDate()));
                if (st == StatusChamado.RESOLVIDO) {
                    b[1]++;
                } else if (st == StatusChamado.CANCELADO) {
                    b[2]++;
                }
            }
        }

        List<PontoSerieResponse> lista = new ArrayList<>();
        long totalAbertos = 0;
        long totalResolvidos = 0;
        for (Map.Entry<LocalDate, long[]> e : baldes.entrySet()) {
            long[] v = e.getValue();
            totalAbertos += v[0];
            totalResolvidos += v[1];
            lista.add(new PontoSerieResponse(e.getKey(), tipo.rotulo(e.getKey()), v[0], v[1], v[2]));
        }
        return new SerieTemporalResponse(tipo.nome, inicio, fim, totalAbertos, totalResolvidos, lista);
    }

    // ------------------------------------------------------------------ RF03

    public List<ContagemStatusResponse> distribuicaoPorStatus(FiltroChamados filtro) {
        List<Document> grupos = agrupar(filtro, "$status", null);
        Map<String, Long> contagem = new LinkedHashMap<>();
        Arrays.stream(StatusChamado.values()).forEach(s -> contagem.put(s.getRotulo(), 0L));
        long total = 0;
        for (Document g : grupos) {
            String rotulo = StatusChamado.de(g.getString("_id")).map(StatusChamado::getRotulo).orElse(NAO_INFORMADO);
            long qtd = ((Number) g.get("total")).longValue();
            contagem.merge(rotulo, qtd, Long::sum);
            total += qtd;
        }
        final long totalGeral = total;
        return contagem.entrySet().stream()
                .filter(e -> !e.getKey().equals(NAO_INFORMADO) || e.getValue() > 0)
                .map(e -> new ContagemStatusResponse(e.getKey(), e.getValue(), percentual(e.getValue(), totalGeral),
                        StatusChamado.de(e.getKey()).map(StatusChamado::isFinalizado).orElse(false)))
                .toList();
    }

    // ------------------------------------------------------------------ RF04

    public List<RankingItemResponse> rankingTipos(FiltroChamados filtro, int limite) {
        return ranking(filtro, "$tipo", limite);
    }

    public List<RankingItemResponse> rankingTecnicos(FiltroChamados filtro, int limite) {
        return ranking(filtro, "$tecnico", limite);
    }

    private List<RankingItemResponse> ranking(FiltroChamados filtro, String campo, int limite) {
        if (limite < 1 || limite > 50) {
            throw new RequisicaoInvalidaException("limite deve estar entre 1 e 50");
        }
        Document extras = new Document()
                .append("resolvidos", new Document("$sum", new Document("$cond",
                        List.of(new Document("$eq", List.of("$status", StatusChamado.RESOLVIDO.getRotulo())), 1, 0))))
                .append("tempoMedioMs", new Document("$avg", new Document("$cond", Arrays.asList(
                        new Document("$and", List.of(
                                new Document("$eq", List.of("$status", StatusChamado.RESOLVIDO.getRotulo())),
                                new Document("$gt", Arrays.asList("$dataFechamento", null)),
                                new Document("$gt", Arrays.asList("$dataAbertura", null)))),
                        new Document("$subtract", List.of("$dataFechamento", "$dataAbertura")),
                        null))));
        List<Document> grupos = agrupar(filtro, campo, extras);
        long total = grupos.stream().mapToLong(g -> ((Number) g.get("total")).longValue()).sum();

        // Mescla grupos que só diferem por espaços/caixa e trata valores ausentes.
        Map<String, long[]> mesclado = new LinkedHashMap<>();
        Map<String, double[]> tempos = new LinkedHashMap<>();
        for (Document g : grupos) {
            Object id = g.get("_id");
            String nome = id == null || id.toString().isBlank() ? NAO_INFORMADO : id.toString().trim();
            long qtd = ((Number) g.get("total")).longValue();
            long res = ((Number) g.get("resolvidos")).longValue();
            long[] acc = mesclado.computeIfAbsent(nome, k -> new long[2]);
            acc[0] += qtd;
            acc[1] += res;
            Number ms = (Number) g.get("tempoMedioMs");
            if (ms != null && res > 0) {
                double[] t = tempos.computeIfAbsent(nome, k -> new double[2]);
                t[0] += ms.doubleValue() * res;
                t[1] += res;
            }
        }

        List<Map.Entry<String, long[]>> ordenado = mesclado.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, long[]>>comparingLong(e -> e.getValue()[0]).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limite)
                .toList();
        List<RankingItemResponse> resultado = new ArrayList<>();
        int posicao = 1;
        for (Map.Entry<String, long[]> e : ordenado) {
            double[] t = tempos.get(e.getKey());
            Double horas = t == null || t[1] == 0 ? null : arredondar(t[0] / t[1] / 3_600_000d);
            resultado.add(new RankingItemResponse(posicao++, e.getKey(), e.getValue()[0], e.getValue()[1],
                    percentual(e.getValue()[0], total), horas));
        }
        return resultado;
    }

    // ------------------------------------------------------------------ RF05

    public FiltrosDisponiveisResponse filtrosDisponiveis() {
        return new FiltrosDisponiveisResponse(
                StatusChamado.rotulos(),
                Prioridade.rotulos(),
                distintos("tipo"),
                distintos("tecnico"),
                distintos("setor"));
    }

    // ------------------------------------------------------------------ apoio

    private List<Document> agrupar(FiltroChamados filtro, String campo, Document acumuladoresExtras) {
        Document grupo = new Document("_id", campo).append("total", new Document("$sum", 1));
        if (acumuladoresExtras != null) {
            grupo.putAll(acumuladoresExtras);
        }
        AggregationOperation group = contexto -> new Document("$group", grupo);
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(FiltroChamadosCriteria.montar(filtro)), group);
        return mongoTemplate.aggregate(agg, COLECAO, Document.class).getMappedResults();
    }

    private List<String> distintos(String campo) {
        return mongoTemplate.findDistinct(new Query(), campo, COLECAO, String.class).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static boolean dentro(LocalDateTime data, LocalDateTime de, LocalDateTime ate) {
        return data != null && !data.isBefore(de) && data.isBefore(ate);
    }

    static double percentual(long parte, long total) {
        return total <= 0 ? 0.0 : arredondar(parte * 100.0 / total);
    }

    static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    /** Agrupamentos aceitos pela série temporal. */
    enum Agrupamento {
        DIA("dia"), SEMANA("semana"), MES("mes");

        final String nome;

        Agrupamento(String nome) {
            this.nome = nome;
        }

        static Agrupamento de(String valor) {
            String v = valor == null || valor.isBlank() ? "dia" : valor.trim().toLowerCase(Locale.ROOT);
            if (v.equals("mês")) {
                v = "mes";
            }
            for (Agrupamento a : values()) {
                if (a.nome.equals(v)) {
                    return a;
                }
            }
            throw new RequisicaoInvalidaException("agrupamento inválido: \"" + valor + "\". Valores aceitos: [dia, semana, mes]");
        }

        LocalDate inicioPadrao(LocalDate fim) {
            return switch (this) {
                case DIA -> fim.minusDays(29);
                case SEMANA -> fim.minusWeeks(11);
                case MES -> fim.minusMonths(11).withDayOfMonth(1);
            };
        }

        LocalDate inicioDoBalde(LocalDate d) {
            return switch (this) {
                case DIA -> d;
                case SEMANA -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case MES -> d.withDayOfMonth(1);
            };
        }

        LocalDate proximo(LocalDate d) {
            return switch (this) {
                case DIA -> d.plusDays(1);
                case SEMANA -> d.plusWeeks(1);
                case MES -> d.plusMonths(1);
            };
        }

        long quantidadeDeBaldes(LocalDate inicio, LocalDate fim) {
            return switch (this) {
                case DIA -> ChronoUnit.DAYS.between(inicio, fim) + 1;
                case SEMANA -> ChronoUnit.WEEKS.between(inicio, fim) + 1;
                case MES -> ChronoUnit.MONTHS.between(inicio, fim) + 1;
            };
        }

        String rotulo(LocalDate d) {
            return switch (this) {
                case DIA -> d.format(DIA_MES);
                case SEMANA -> "Sem. " + d.format(DIA_MES);
                case MES -> d.format(MES_ANO);
            };
        }
    }
}
