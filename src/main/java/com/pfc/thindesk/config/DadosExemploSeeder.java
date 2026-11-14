package com.pfc.thindesk.config;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Prioridade;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@ConditionalOnProperty(name = "thindesk.seed.enabled", havingValue = "true")
public class DadosExemploSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosExemploSeeder.class);

    private static final String[] TIPOS = {"Hardware", "Software", "Rede", "Acesso/Senha", "Impressora", "E-mail", "Manutenção"};
    private static final int[] PESO_TIPOS = {18, 24, 14, 20, 10, 8, 6};
    private static final String[] TECNICOS = {"João", "Mariana", "Carlos", "Fernanda", "Rafael"};
    private static final String[] SETORES = {"Financeiro", "RH", "Comercial", "TI", "Produção", "Logística"};
    private static final String[] USUARIOS = {"Alberto", "Beatriz", "Caio", "Daniela", "Eduardo", "Fabiana", "Gustavo",
            "Helena", "Igor", "Juliana", "Lucas", "Marta", "Nicolas", "Olívia", "Paulo", "Renata"};

    private static final Map<String, String[]> DESCRICOES = Map.of(
            "Hardware", new String[]{"Computador não liga", "Monitor piscando", "Teclado com teclas falhando", "Notebook superaquecendo"},
            "Software", new String[]{"Erro ao abrir o ERP", "Instalação do pacote Office", "Sistema travando ao gerar relatório", "Atualização do antivírus"},
            "Rede", new String[]{"Sem acesso à internet", "Wi-Fi caindo constantemente", "Lentidão na rede interna", "Ponto de rede sem sinal"},
            "Acesso/Senha", new String[]{"Senha expirada", "Usuário bloqueado no AD", "Liberar acesso à pasta compartilhada", "Criar conta para novo colaborador"},
            "Impressora", new String[]{"Impressora não imprime", "Papel atolando", "Instalar driver de impressora", "Toner acabando"},
            "E-mail", new String[]{"Caixa de e-mail cheia", "Não recebe e-mails externos", "Configurar e-mail no celular"},
            "Manutenção", new String[]{"Troca de cabo de rede", "Limpeza preventiva", "Remanejamento de estação de trabalho"});

    private static final Map<String, String[]> CONSIDERACOES = Map.of(
            "Hardware", new String[]{"Fonte substituída.", "Memória RAM recolocada e testada.", "Equipamento enviado para garantia."},
            "Software", new String[]{"Reinstalação do aplicativo resolveu.", "Cache limpo e permissões corrigidas.", "Atualização aplicada."},
            "Rede", new String[]{"Switch reiniciado.", "Cabo do ponto refeito.", "Canal do access point alterado."},
            "Acesso/Senha", new String[]{"Senha redefinida e usuário orientado.", "Conta desbloqueada no AD.", "Permissão concedida pelo gestor."},
            "Impressora", new String[]{"Driver reinstalado.", "Rolete limpo.", "Toner substituído."},
            "E-mail", new String[]{"Arquivamento automático configurado.", "Regra de spam ajustada."},
            "Manutenção", new String[]{"Serviço executado conforme agendado."});

    private final ChamadoRepository repository;
    private final Clock relogio;
    private final int quantidade;

    public DadosExemploSeeder(ChamadoRepository repository, Clock relogio,
            @Value("${thindesk.seed.quantidade:300}") int quantidade) {
        this.repository = repository;
        this.relogio = relogio;
        this.quantidade = quantidade;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("Coleção de chamados já possui dados; carga de exemplo ignorada.");
            return;
        }
        repository.saveAll(gerar(quantidade, LocalDateTime.now(relogio), new Random(42)));
        log.info("{} chamados de exemplo inseridos.", quantidade);
    }

    static List<Chamado> gerar(int quantidade, LocalDateTime agora, Random rnd) {
        List<Chamado> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            double r = Math.sqrt(rnd.nextDouble());
            long minutosAtras = (long) ((1 - r) * 180 * 24 * 60);
            LocalDateTime abertura = agora.minusMinutes(minutosAtras).truncatedTo(ChronoUnit.MINUTES);
            if (abertura.getHour() < 8 || abertura.getHour() > 18) {
                abertura = abertura.withHour(8 + rnd.nextInt(10));
                if (abertura.isAfter(agora)) {
                    abertura = abertura.minusDays(1);
                }
            }

            String tipo = sortearTipo(rnd);
            Prioridade prioridade = sortearPrioridade(rnd);
            long horasDesdeAbertura = ChronoUnit.HOURS.between(abertura, agora);

            Chamado c = new Chamado();
            c.setTipo(tipo);
            c.setDescricao(sortear(DESCRICOES.get(tipo), rnd));
            c.setTecnico(sortear(TECNICOS, rnd));
            c.setSetor(sortear(SETORES, rnd));
            c.setUsuario(sortear(USUARIOS, rnd));
            c.setPrioridade(prioridade.getRotulo());
            c.setDataAbertura(abertura);

            double fator = 0.15 + rnd.nextDouble() * (rnd.nextDouble() < 0.2 ? 2.5 : 0.95);
            long minutosResolucao = Math.max(20, (long) (prioridade.getSlaHoras() * 60 * fator));
            LocalDateTime fechamento = abertura.plusMinutes(minutosResolucao);

            StatusChamado status;
            if (fechamento.isBefore(agora) && rnd.nextDouble() < (horasDesdeAbertura > 72 ? 0.97 : 0.6)) {
                status = rnd.nextDouble() < 0.06 ? StatusChamado.CANCELADO : StatusChamado.RESOLVIDO;
                c.setDataFechamento(fechamento);
                c.setConsideracoesTecnicas(status == StatusChamado.CANCELADO
                        ? "Cancelado a pedido do usuário."
                        : sortear(CONSIDERACOES.get(tipo), rnd));
            } else {
                double x = rnd.nextDouble();
                status = x < 0.45 ? StatusChamado.ABERTO : x < 0.85 ? StatusChamado.EM_ANDAMENTO : StatusChamado.AGUARDANDO;
                if (status != StatusChamado.ABERTO) {
                    c.setConsideracoesTecnicas(status == StatusChamado.AGUARDANDO
                            ? "Aguardando retorno do usuário/peça."
                            : "Em análise pelo técnico.");
                }
            }
            c.setStatus(status.getRotulo());
            lista.add(c);
        }
        return lista;
    }

    private static String sortearTipo(Random rnd) {
        int soma = 0;
        for (int p : PESO_TIPOS) {
            soma += p;
        }
        int alvo = rnd.nextInt(soma);
        for (int i = 0; i < TIPOS.length; i++) {
            alvo -= PESO_TIPOS[i];
            if (alvo < 0) {
                return TIPOS[i];
            }
        }
        return TIPOS[0];
    }

    private static Prioridade sortearPrioridade(Random rnd) {
        double x = rnd.nextDouble();
        return x < 0.30 ? Prioridade.BAIXA : x < 0.70 ? Prioridade.MEDIA : x < 0.92 ? Prioridade.ALTA : Prioridade.CRITICA;
    }

    private static String sortear(String[] opcoes, Random rnd) {
        return opcoes[rnd.nextInt(opcoes.length)];
    }
}
