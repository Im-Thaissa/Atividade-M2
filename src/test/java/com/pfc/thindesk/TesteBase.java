package com.pfc.thindesk;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TesteBase.RelogioFixo.class)
public abstract class TesteBase {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 14, 12, 0);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ChamadoRepository repository;

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);
        }
    }

    protected static Chamado chamado(String tipo, String tecnico, String setor, String prioridade, String status,
            LocalDateTime abertura, LocalDateTime fechamento) {
        Chamado c = new Chamado();
        c.setDescricao("Chamado de teste - " + tipo);
        c.setUsuario("Usuário Teste");
        c.setTipo(tipo);
        c.setTecnico(tecnico);
        c.setSetor(setor);
        c.setPrioridade(prioridade);
        c.setStatus(status);
        c.setDataAbertura(abertura);
        c.setDataFechamento(fechamento);
        return c;
    }

    protected static LocalDateTime dia(int mes, int dia, int hora) {
        return LocalDateTime.of(2026, mes, dia, hora, 0);
    }
}
