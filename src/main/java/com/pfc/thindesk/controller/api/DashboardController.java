package com.pfc.thindesk.controller.api;

import com.pfc.thindesk.dto.ContagemStatusResponse;
import com.pfc.thindesk.dto.FiltroChamados;
import com.pfc.thindesk.dto.FiltrosDisponiveisResponse;
import com.pfc.thindesk.dto.IndicadoresResponse;
import com.pfc.thindesk.dto.RankingItemResponse;
import com.pfc.thindesk.dto.SerieTemporalResponse;
import com.pfc.thindesk.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores(@ModelAttribute FiltroChamados filtro) {
        return dashboardService.indicadores(filtro);
    }

    @GetMapping("/chamados-por-periodo")
    public SerieTemporalResponse porPeriodo(@ModelAttribute FiltroChamados filtro,
            @RequestParam(defaultValue = "dia") String agrupamento) {
        return dashboardService.seriePorPeriodo(filtro, agrupamento);
    }

    @GetMapping("/status")
    public List<ContagemStatusResponse> status(@ModelAttribute FiltroChamados filtro) {
        return dashboardService.distribuicaoPorStatus(filtro);
    }

    @GetMapping("/tipos")
    public List<RankingItemResponse> tipos(@ModelAttribute FiltroChamados filtro,
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.rankingTipos(filtro, limite);
    }

    @GetMapping("/tecnicos")
    public List<RankingItemResponse> tecnicos(@ModelAttribute FiltroChamados filtro,
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.rankingTecnicos(filtro, limite);
    }

    @GetMapping("/filtros")
    public FiltrosDisponiveisResponse filtros() {
        return dashboardService.filtrosDisponiveis();
    }
}
