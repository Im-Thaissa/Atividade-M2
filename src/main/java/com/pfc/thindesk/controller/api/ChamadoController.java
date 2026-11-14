package com.pfc.thindesk.controller.api;

import com.pfc.thindesk.dto.AtualizacaoStatusRequest;
import com.pfc.thindesk.dto.ChamadoRequest;
import com.pfc.thindesk.dto.FiltroChamados;
import com.pfc.thindesk.dto.PaginaResponse;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de chamados: consulta (com filtros e paginação), criação e atualização. */
@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @GetMapping
    public PaginaResponse<Chamado> listar(@ModelAttribute FiltroChamados filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "dataAbertura") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao) {
        return chamadoService.pesquisar(filtro, pagina, tamanho, ordenarPor, direcao);
    }

    @GetMapping("/{id}")
    public Chamado buscar(@PathVariable String id) {
        return chamadoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Chamado> criar(@Valid @RequestBody ChamadoRequest req) {
        Chamado criado = chamadoService.criar(req);
        return ResponseEntity.created(URI.create("/api/chamados/" + criado.getId())).body(criado);
    }

    @PutMapping("/{id}")
    public Chamado atualizar(@PathVariable String id, @Valid @RequestBody ChamadoRequest req) {
        return chamadoService.atualizar(id, req);
    }

    @PatchMapping("/{id}/status")
    public Chamado atualizarStatus(@PathVariable String id, @Valid @RequestBody AtualizacaoStatusRequest req) {
        return chamadoService.atualizarStatus(id, req.status(), req.consideracoesTecnicas());
    }

    /** Mantido para o formulário HTML legado (chamados/atualizar.html). */
    @PostMapping("/atualizar/{id}")
    public ResponseEntity<Void> atualizarViaFormulario(@PathVariable String id, @ModelAttribute Chamado dados) {
        chamadoService.atualizarChamado(id, dados);
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/chamados")).build();
    }
}
