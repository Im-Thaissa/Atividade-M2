// Dashboard Administrativo - Thindesk
// Consome a API REST (/api/dashboard/** e /api/chamados) e renderiza indicadores, gráficos e tabelas.
(function () {
    'use strict';

    const INTERVALO_ATUALIZACAO_MS = 60000;
    const TAMANHO_PAGINA = 8;
    const ICONES_STATUS = {
        'Aberto': 'bi-circle',
        'Em andamento': 'bi-arrow-repeat',
        'Aguardando': 'bi-pause-circle',
        'Resolvido': 'bi-check-circle-fill',
        'Cancelado': 'bi-x-circle'
    };

    const $ = (id) => document.getElementById(id);
    const numero = new Intl.NumberFormat('pt-BR');
    const decimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
    const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

    const estado = { pagina: 0, timer: null, graficos: {}, carregando: false };

    // ------------------------------------------------------------ utilitários

    function cssVar(nome) {
        return getComputedStyle(document.documentElement).getPropertyValue(nome).trim();
    }

    function escapar(texto) {
        const div = document.createElement('div');
        div.textContent = texto == null ? '' : String(texto);
        return div.innerHTML;
    }

    function horas(h) {
        if (h == null) return '–';
        if (h < 1) return Math.round(h * 60) + ' min';
        if (h >= 48) return decimal.format(h / 24) + ' dias';
        return decimal.format(h) + ' h';
    }

    function isoLocal(d) {
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const dia = String(d.getDate()).padStart(2, '0');
        return d.getFullYear() + '-' + m + '-' + dia;
    }

    /** Chamada à API com tratamento de erros padronizado (RF06/RF07). */
    async function api(caminho, params) {
        const url = new URL(caminho, window.location.origin);
        Object.entries(params || {}).forEach(([k, v]) => {
            if (v !== '' && v != null) url.searchParams.set(k, v);
        });
        let resp;
        try {
            resp = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        } catch (e) {
            throw new Error('Não foi possível conectar à API. Verifique se o servidor está em execução.');
        }
        if (resp.status === 401) {
            window.location.href = '/login';
            throw new Error('Sessão expirada.');
        }
        const corpo = await resp.json().catch(() => null);
        if (!resp.ok) {
            const msg = corpo && corpo.mensagem ? corpo.mensagem : 'Erro ' + resp.status + ' ao consultar a API.';
            const det = corpo && corpo.detalhes && corpo.detalhes.length ? ' (' + corpo.detalhes.join('; ') + ')' : '';
            throw new Error(msg + det);
        }
        return corpo;
    }

    // ------------------------------------------------------------ filtros (RF05)

    function aplicarPeriodoPreset() {
        const preset = $('fPeriodo').value;
        const custom = preset === 'custom';
        $('fInicio').disabled = !custom;
        $('fFim').disabled = !custom;
        if (!custom) {
            const fim = new Date();
            const inicio = new Date();
            inicio.setDate(fim.getDate() - (Number(preset) - 1));
            $('fInicio').value = isoLocal(inicio);
            $('fFim').value = isoLocal(fim);
        }
    }

    function lerFiltros() {
        const f = { dataInicio: $('fInicio').value, dataFim: $('fFim').value };
        document.querySelectorAll('[data-param]').forEach((el) => { f[el.dataset.param] = el.value; });
        return f;
    }

    function agrupamento() {
        return document.querySelector('input[name="agrupamento"]:checked').value;
    }

    /** Mantém os filtros na URL para que a visão possa ser compartilhada. */
    function salvarNaUrl(f) {
        const p = new URLSearchParams();
        p.set('periodo', $('fPeriodo').value);
        Object.entries(f).forEach(([k, v]) => { if (v) p.set(k, v); });
        p.set('agrupamento', agrupamento());
        history.replaceState(null, '', '?' + p.toString());
    }

    function restaurarDaUrl() {
        const p = new URLSearchParams(window.location.search);
        if (p.get('periodo')) $('fPeriodo').value = p.get('periodo');
        aplicarPeriodoPreset();
        if ($('fPeriodo').value === 'custom') {
            if (p.get('dataInicio')) $('fInicio').value = p.get('dataInicio');
            if (p.get('dataFim')) $('fFim').value = p.get('dataFim');
        }
        document.querySelectorAll('[data-param]').forEach((el) => {
            const v = p.get(el.dataset.param);
            if (v) {
                if (![...el.options].some((o) => o.value === v)) el.add(new Option(v, v));
                el.value = v;
            }
        });
        const ag = p.get('agrupamento');
        if (ag && document.querySelector('input[name="agrupamento"][value="' + ag + '"]')) {
            document.querySelector('input[name="agrupamento"][value="' + ag + '"]').checked = true;
        }
    }

    async function carregarOpcoesFiltro() {
        const op = await api('/api/dashboard/filtros');
        preencher($('fStatus'), op.status);
        preencher($('fTipo'), op.tipos);
        preencher($('fTecnico'), op.tecnicos);
        preencher($('fPrioridade'), op.prioridades);
        preencher($('fSetor'), op.setores);
    }

    function preencher(select, valores) {
        const atual = select.value;
        const primeiro = select.options[0];
        select.innerHTML = '';
        select.add(primeiro);
        valores.forEach((v) => select.add(new Option(v, v)));
        select.value = valores.includes(atual) ? atual : '';
    }

    // ------------------------------------------------------------ renderização

    function renderIndicadores(k) {
        $('kTotal').textContent = numero.format(k.total);
        $('kHoje').textContent = numero.format(k.abertosHoje) + ' aberto(s) hoje';
        $('kPendentes').textContent = numero.format(k.pendentes);
        $('kPendDet').textContent = k.abertos + ' abertos · ' + k.emAndamento + ' em andamento · ' + k.aguardando + ' aguardando';
        $('kResolvidos').textContent = numero.format(k.resolvidos);
        $('kCancelados').textContent = numero.format(k.cancelados) + ' cancelado(s)';
        $('kTaxa').textContent = decimal.format(k.taxaResolucao) + '%';
        $('kTempo').textContent = horas(k.tempoMedioResolucaoHoras);
        $('kSla').textContent = k.percentualResolvidosNoSla == null ? '–' : decimal.format(k.percentualResolvidosNoSla) + '%';
        $('kCriticos').textContent = numero.format(k.criticosPendentes);
        $('kForaSla').textContent = numero.format(k.pendentesForaDoSla);
    }

    function opcoesBase() {
        const tinta = cssVar('--ink-2');
        const grade = cssVar('--grid');
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 250 },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: cssVar('--surface-1'),
                    titleColor: cssVar('--ink-1'),
                    bodyColor: cssVar('--ink-1'),
                    borderColor: cssVar('--axis'),
                    borderWidth: 1,
                    padding: 10,
                    usePointStyle: true
                }
            },
            scales: {
                x: { grid: { display: false }, border: { color: cssVar('--axis') }, ticks: { color: tinta, maxRotation: 0, autoSkipPadding: 12 } },
                y: { beginAtZero: true, grid: { color: grade }, border: { display: false }, ticks: { color: tinta, precision: 0 } }
            }
        };
    }

    function desenhar(chave, canvasId, config) {
        if (estado.graficos[chave]) estado.graficos[chave].destroy();
        estado.graficos[chave] = new Chart($(canvasId), config);
    }

    function renderSerie(s) {
        const nomeAg = { dia: 'dia', semana: 'semana', mes: 'mês' }[s.agrupamento];
        $('serieResumo').textContent = numero.format(s.totalAbertos) + ' abertos e ' + numero.format(s.totalResolvidos)
            + ' resolvidos, por ' + nomeAg;
        const opcoes = opcoesBase();
        opcoes.interaction = { mode: 'index', intersect: false };
        const ponto = (cor) => ({ borderColor: cor, backgroundColor: cor, borderWidth: 2, pointRadius: 0, pointHoverRadius: 5, pointHoverBorderWidth: 2, pointHoverBorderColor: cssVar('--surface-1'), cubicInterpolationMode: 'monotone' });
        desenhar('serie', 'graficoSerie', {
            type: 'line',
            data: {
                labels: s.pontos.map((p) => p.rotulo),
                datasets: [
                    Object.assign({ label: 'Abertos', data: s.pontos.map((p) => p.abertos) }, ponto(cssVar('--serie-1'))),
                    Object.assign({ label: 'Resolvidos', data: s.pontos.map((p) => p.resolvidos) }, ponto(cssVar('--serie-2')))
                ]
            },
            options: opcoes
        });
        $('tabSerie').innerHTML = s.pontos.map((p) => '<tr><td>' + escapar(p.rotulo) + '</td><td class="num">' + p.abertos
            + '</td><td class="num">' + p.resolvidos + '</td><td class="num">' + p.cancelados + '</td></tr>').join('');
    }

    function barrasHorizontais(chave, canvasId, rotulos, valores, nomeSerie) {
        const opcoes = opcoesBase();
        opcoes.indexAxis = 'y';
        opcoes.scales = {
            x: { beginAtZero: true, grid: { color: cssVar('--grid') }, border: { display: false }, ticks: { color: cssVar('--ink-2'), precision: 0 } },
            y: { grid: { display: false }, border: { color: cssVar('--axis') }, ticks: { color: cssVar('--ink-1') } }
        };
        desenhar(chave, canvasId, {
            type: 'bar',
            data: {
                labels: rotulos,
                datasets: [{
                    label: nomeSerie,
                    data: valores,
                    backgroundColor: cssVar('--serie-1'),
                    borderRadius: { topRight: 4, bottomRight: 4 },
                    borderSkipped: 'left',
                    barPercentage: 0.7,
                    categoryPercentage: 0.9
                }]
            },
            options: opcoes
        });
    }

    function badge(status) {
        const icone = ICONES_STATUS[status] || 'bi-question-circle';
        return '<span class="badge-status"><i class="bi ' + icone + '" aria-hidden="true"></i>' + escapar(status || 'Não informado') + '</span>';
    }

    function renderStatus(lista) {
        barrasHorizontais('status', 'graficoStatus', lista.map((s) => s.status), lista.map((s) => s.quantidade), 'Chamados');
        $('tabStatus').innerHTML = lista.map((s) => '<tr><td>' + badge(s.status) + '</td><td class="num">'
            + numero.format(s.quantidade) + '</td><td class="num">' + decimal.format(s.percentual) + '%</td></tr>').join('');
    }

    function linhasRanking(lista, vazio) {
        if (!lista.length) return '<tr><td colspan="5" class="vazio">' + vazio + '</td></tr>';
        return lista.map((t) => '<tr><td>' + t.posicao + '</td><td>' + escapar(t.nome) + '</td><td class="num">'
            + numero.format(t.total) + '</td><td class="num">' + numero.format(t.resolvidos) + '</td><td class="num">'
            + horas(t.tempoMedioResolucaoHoras) + '</td></tr>').join('');
    }

    function renderTipos(lista) {
        barrasHorizontais('tipos', 'graficoTipos', lista.map((t) => t.nome), lista.map((t) => t.total), 'Chamados');
        $('tabTipos').innerHTML = linhasRanking(lista, 'Nenhum chamado para os filtros selecionados.');
    }

    function renderTecnicos(lista) {
        $('tabTecnicos').innerHTML = linhasRanking(lista, 'Nenhum atendimento para os filtros selecionados.');
    }

    function renderRecentes(pagina) {
        $('recentesTotal').textContent = numero.format(pagina.totalElementos) + ' chamado(s)';
        if (!pagina.conteudo.length) {
            $('tabRecentes').innerHTML = '<tr><td colspan="5" class="vazio">Nenhum chamado para os filtros selecionados.</td></tr>';
        } else {
            $('tabRecentes').innerHTML = pagina.conteudo.map((c) => '<tr><td>'
                + (c.dataAbertura ? dataHora.format(new Date(c.dataAbertura)) : '–') + '</td><td>' + escapar(c.tipo || 'Não informado')
                + '</td><td>' + badge(c.status) + '</td><td>' + escapar(c.tecnico || '–') + '</td><td title="'
                + escapar(c.consideracoesTecnicas || '') + '">' + escapar(c.consideracoesTecnicas || '–') + '</td></tr>').join('');
        }
        const totalPaginas = Math.max(pagina.totalPaginas, 1);
        $('pagInfo').textContent = 'Página ' + (pagina.pagina + 1) + ' de ' + totalPaginas;
        $('pagAnterior').disabled = pagina.pagina <= 0;
        $('pagProxima').disabled = pagina.pagina + 1 >= totalPaginas;
    }

    // ------------------------------------------------------------ carregamento (RF06/RF07)

    async function carregarRecentes(filtros) {
        const pagina = await api('/api/chamados', Object.assign({}, filtros, { pagina: estado.pagina, tamanho: TAMANHO_PAGINA }));
        renderRecentes(pagina);
    }

    async function carregarTudo() {
        if (estado.carregando) return;
        estado.carregando = true;
        const filtros = lerFiltros();
        salvarNaUrl(filtros);
        document.querySelector('.dash').classList.add('carregando');
        $('alerta').classList.add('d-none');
        try {
            const [indicadores, serie, status, tipos, tecnicos] = await Promise.all([
                api('/api/dashboard/indicadores', filtros),
                api('/api/dashboard/chamados-por-periodo', Object.assign({}, filtros, { agrupamento: agrupamento() })),
                api('/api/dashboard/status', filtros),
                api('/api/dashboard/tipos', Object.assign({}, filtros, { limite: 7 })),
                api('/api/dashboard/tecnicos', Object.assign({}, filtros, { limite: 10 })),
                carregarRecentes(filtros)
            ]);
            renderIndicadores(indicadores);
            renderSerie(serie);
            renderStatus(status);
            renderTipos(tipos);
            renderTecnicos(tecnicos);
            $('ultimaAtualizacao').textContent = 'Atualizado às ' + new Date().toLocaleTimeString('pt-BR');
        } catch (e) {
            $('alerta').textContent = e.message;
            $('alerta').classList.remove('d-none');
            $('ultimaAtualizacao').textContent = 'Falha na última atualização';
        } finally {
            estado.carregando = false;
            document.querySelector('.dash').classList.remove('carregando');
        }
    }

    function configurarAutoAtualizacao() {
        clearInterval(estado.timer);
        if ($('autoAtualizar').checked) {
            estado.timer = setInterval(() => {
                if (!document.hidden) carregarTudo();
            }, INTERVALO_ATUALIZACAO_MS);
        }
    }

    // ------------------------------------------------------------ eventos

    function iniciar() {
        restaurarDaUrl();

        $('fPeriodo').addEventListener('change', aplicarPeriodoPreset);
        $('formFiltros').addEventListener('submit', (ev) => {
            ev.preventDefault();
            estado.pagina = 0;
            carregarTudo();
        });
        $('btnLimpar').addEventListener('click', () => {
            $('fPeriodo').value = '30';
            aplicarPeriodoPreset();
            document.querySelectorAll('[data-param]').forEach((el) => { el.value = ''; });
            estado.pagina = 0;
            carregarTudo();
        });
        document.querySelectorAll('input[name="agrupamento"]').forEach((r) => r.addEventListener('change', carregarTudo));
        $('btnAtualizar').addEventListener('click', () => { carregarOpcoesFiltro().catch(() => {}); carregarTudo(); });
        $('autoAtualizar').addEventListener('change', configurarAutoAtualizacao);
        $('pagAnterior').addEventListener('click', () => { estado.pagina--; carregarRecentes(lerFiltros()).catch(mostrarErro); });
        $('pagProxima').addEventListener('click', () => { estado.pagina++; carregarRecentes(lerFiltros()).catch(mostrarErro); });
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', carregarTudo);

        carregarOpcoesFiltro()
            .then(restaurarDaUrl)
            .catch(mostrarErro)
            .finally(carregarTudo);
        configurarAutoAtualizacao();
    }

    function mostrarErro(e) {
        $('alerta').textContent = e.message;
        $('alerta').classList.remove('d-none');
    }

    document.addEventListener('DOMContentLoaded', iniciar);
})();
