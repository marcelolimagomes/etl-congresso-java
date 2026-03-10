package br.leg.congresso.etl.loader.silver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraApiExtractor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAutoriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDocumentoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoEmendaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPrazoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRelatoriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoVotacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de enriquecimento Silver.
 *
 * Após a carga inicial dos registros Silver (passthrough da fonte principal),
 * este serviço consulta endpoints secundários para preencher:
 *
 * <ul>
 * <li>Senado detalhe: campos {@code det_*} em
 * {@code silver.senado_materia}</li>
 * <li>Câmara tramitações: registros em {@code silver.camara_tramitacao}</li>
 * <li>Senado movimentações: registros em
 * {@code silver.senado_movimentacao}</li>
 * </ul>
 *
 * Cada operação é idempotente: registros já enriquecidos são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverEnrichmentService {

    private final SilverSenadoMateriaRepository silverSenadoRepository;
    private final SilverCamaraProposicaoRepository silverCamaraRepository;
    private final SenadoApiExtractor senadoApiExtractor;
    private final CamaraApiExtractor camaraApiExtractor;
    private final SilverCamaraTramitacaoLoader tramitacaoLoader;
    private final SilverSenadoMovimentacaoLoader movimentacaoLoader;
    private final SilverSenadoAutoriaLoader autoriaLoader;
    private final SilverSenadoRelatoriaLoader relatoriaLoader;
    private final SilverCamaraRelacionadasLoader relacionadasLoader;
    private final SilverSenadoEmendaLoader emendaLoader;
    private final SilverSenadoDocumentoLoader documentoLoader;
    private final SilverSenadoPrazoLoader prazoLoader;
    private final SilverSenadoVotacaoLoader votacaoLoader;
    private final EtlJobControlService jobControlService;
    private final ObjectMapper objectMapper;
    private final @Qualifier("etlExecutor") Executor etlExecutor;
    private final SilverSenadoEnriquecedor senadoEnriquecedor;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    @Value("${etl.senado.enrichment.parallelism:6}")
    private int senadoEnrichmentParallelism;

    // ─── Enriquecimento Senado Detalhe ─────────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com dados do endpoint de detalhe.
     * Apenas registros com {@code det_sigla_casa_identificacao IS NULL} são
     * processados.
     *
     * @return número de registros efetivamente enriquecidos
     */
    public int enriquecerDetalhesSenado() {
        List<SilverSenadoMateria> pendentes = silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull();
        log.info("[Enrich] {} registros Silver Senado pendentes de enriquecimento de detalhe", pendentes.size());
        int loteSize = resolveBatchSize();
        int paralelismo = resolveSenadoEnrichmentParallelism();

        AtomicInteger enriquecidos = new AtomicInteger(0);
        for (int i = 0; i < pendentes.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, pendentes.size());
            List<SilverSenadoMateria> lote = pendentes.subList(i, fim);
            List<List<SilverSenadoMateria>> grupos = partitionBySize(lote, paralelismo);

            List<CompletableFuture<Void>> futures = new ArrayList<>(grupos.size());
            for (List<SilverSenadoMateria> grupo : grupos) {
                futures.add(CompletableFuture.runAsync(() -> processarGrupoDetalheSenado(grupo, enriquecidos),
                        etlExecutor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }

        int totalEnriquecidos = enriquecidos.get();
        log.info("[Enrich] Senado detalhe concluído: {}/{} registros enriquecidos", totalEnriquecidos,
                pendentes.size());
        return totalEnriquecidos;
    }

    private void processarGrupoDetalheSenado(List<SilverSenadoMateria> grupo, AtomicInteger enriquecidos) {
        for (SilverSenadoMateria silver : grupo) {
            try {
                SenadoDetalheDTO detalhe = senadoApiExtractor.fetchRawDetalhe(silver.getCodigo());
                if (detalhe != null) {
                    boolean enriched = senadoEnriquecedor.enriquecer(
                            silver.getCodigo(), entity -> applyDetalhe(entity, detalhe));
                    if (enriched)
                        enriquecidos.incrementAndGet();
                }
            } catch (Exception e) {
                log.warn("[Enrich] Falha ao enriquecer Senado código={}: {}", silver.getCodigo(), e.getMessage());
            }
        }
    }

    // ─── Enriquecimento Câmara Tramitações ────────────────────────────────────

    /**
     * Enriquece registros Silver da Câmara com tramitações do endpoint REST.
     * Tramitações já existentes são ignoradas (idempotente).
     *
     * @param proposicoes lista de proposições Silver a enriquecer; usa todas se
     *                    null
     * @return total de novas tramitações inseridas
     */
    public int enriquecerTramitacoesCamara(List<SilverCamaraProposicao> proposicoes) {
        List<SilverCamaraProposicao> alvo = (proposicoes != null)
                ? proposicoes
                : silverCamaraRepository.findAll();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} proposições Silver Câmara para enriquecimento de tramitações", alvo.size());

        int totalInseridas = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverCamaraProposicao> lote = alvo.subList(i, fim);

            for (SilverCamaraProposicao prop : lote) {
                if (prop.getCamaraId() == null || prop.getCamaraId().isBlank())
                    continue;
                try {
                    List<CamaraTramitacaoDTO> trams = camaraApiExtractor.fetchTramitacoesRaw(prop.getCamaraId());
                    totalInseridas += tramitacaoLoader.carregar(prop, trams);
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar tramitações Câmara camaraId={}: {}",
                            prop.getCamaraId(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Câmara tramitações: {} novas inseridas para {} proposições",
                totalInseridas, alvo.size());
        return totalInseridas;
    }

    // ─── Enriquecimento Senado Movimentações ─────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com movimentações do endpoint REST.
     * Movimentações já existentes são ignoradas (idempotente).
     *
     * @param materias lista de matérias Silver a enriquecer; usa todas se null
     * @return total de novas movimentações inseridas
     */
    public int enriquecerMovimentacoesSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findAll();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de movimentações", alvo.size());

        int totalInseridas = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoMovimentacaoDTO.Movimentacao> movs = senadoApiExtractor
                            .fetchRawMovimentacoes(materia.getCodigo());
                    totalInseridas += movimentacaoLoader.carregar(materia, movs);
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar movimentações Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado movimentações: {} novas inseridas para {} matérias",
                totalInseridas, alvo.size());
        return totalInseridas;
    }

    // ─── Orquestração completa ────────────────────────────────────────────────

    /**
     * Executa o enriquecimento completo (detalhes, tramitações e movimentações).
     *
     * @return EtlJobControl com os contadores da operação
     */
    public EtlJobControl enriquecerTudo() {
        log.info("[Enrich] Iniciando enriquecimento Silver completo");

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.SENADO,
                TipoExecucao.ENRICHMENT,
                Map.of("operacao", "silver_enrichment", "escopo", "ambas_casas"));

        try {
            // Fases originais
            int detalhes = enriquecerDetalhesSenado();
            int tramitacoes = enriquecerTramitacoesCamara(null);
            int movimentacoes = enriquecerMovimentacoesSenado(null);
            int autorias = enriquecerAutoriasSenado(null);
            int relatorias = enriquecerRelatoriasSenado(null);

            // Fase 5 — Câmara relacionadas (API-only)
            int relacionadas = enriquecerRelacionadasCamara(null);

            // Fase 6 — Senado: emendas, documentos, prazos
            int emendas = enriquecerEmendasSenado(null);
            int documentos = enriquecerDocumentosSenado(null);
            int prazos = enriquecerPrazosSenado(null);

            // Fase 7 — Senado: votações
            int votacoes = enriquecerVotacoesSenado(null);

            int totalInserido = detalhes + tramitacoes + movimentacoes + autorias + relatorias
                    + relacionadas + emendas + documentos + prazos + votacoes;
            for (int i = 0; i < totalInserido; i++) {
                job.incrementarInserido();
                job.incrementarProcessado();
            }

            log.info(
                    "[Enrich] Concluído: {} detalhes Senado, {} tramitações Câmara, {} movimentações Senado, "
                            + "{} autorias, {} relatorias, {} relacionadas Câmara, {} emendas, "
                            + "{} documentos, {} prazos, {} votações Senado",
                    detalhes, tramitacoes, movimentacoes, autorias, relatorias,
                    relacionadas, emendas, documentos, prazos, votacoes);

            return jobControlService.finalizar(job);
        } catch (Exception e) {
            log.error("[Enrich] Falha crítica no enriquecimento Silver: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    // ─── Enriquecimento Senado Autoria ────────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com dados de autoria.
     * Processa somente matérias que ainda não possuem autoria registrada
     * (sem registro em silver.senado_autoria) e que já foram enriquecidas
     * com detalhe (det_sigla_casa_identificacao não nulo).
     *
     * @param materias lista de matérias a processar; usa todas se null
     * @return total de novos registros de autoria inseridos
     */
    public int enriquecerAutoriasSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de autoria", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoAutoriaDTO.Autor> autores = senadoApiExtractor.fetchRawAutoria(materia.getCodigo());
                    totalInseridos += autoriaLoader.carregar(materia, autores);
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar autoria Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado autoria: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Enriquecimento Senado Relatoria ──────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com dados de relatoria.
     * Processa matérias que já foram enriquecidas com detalhe
     * (det_sigla_casa_identificacao não nulo).
     *
     * @param materias lista de matérias a processar; usa todas se null
     * @return total de novos registros de relatoria inseridos
     */
    public int enriquecerRelatoriasSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de relatoria", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoRelatoriaDTO> relatorias = senadoApiExtractor.fetchRelatorias(materia.getCodigo());
                    totalInseridos += relatoriaLoader.carregar(materia, relatorias);
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar relatorias Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado relatoria: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Aplica os campos do endpoint de detalhe no registro Silver.
     * Passthrough: sem normalização de nenhum campo.
     */
    public void applyDetalhe(SilverSenadoMateria silver, SenadoDetalheDTO detalhe) {
        if (detalhe == null || detalhe.getDetalheMateria() == null
                || detalhe.getDetalheMateria().getMateria() == null)
            return;

        var materia = detalhe.getDetalheMateria().getMateria();

        // Campos de identificação estão em IdentificacaoMateria (não em
        // DadosBasicosMateria)
        var identif = materia.getIdentificacaoMateria();
        if (identif != null) {
            silver.setDetSiglaCasaIdentificacao(identif.getSiglaCasaIdentificacao());
            silver.setDetSiglaSubtipo(identif.getSiglaSubtipo());
            silver.setDetDescricaoSubtipo(identif.getDescricaoSubtipo());
            silver.setDetDescricaoObjetivoProcesso(identif.getDescricaoObjetivoProcesso());
            silver.setDetIndicadorTramitando(identif.getIndicadorTramitando());
        }

        var dados = materia.getDadosBasicosMateria();
        if (dados != null) {
            silver.setDetIndexacao(dados.getIndexacao());
            silver.setDetIndicadorComplementar(dados.getIndicadorComplementar());
            // Usa siglaCasaIniciadora se disponível, senão nome
            if (dados.getSiglaCasaIniciadora() != null && !dados.getSiglaCasaIniciadora().isBlank()) {
                silver.setDetCasaIniciadora(dados.getSiglaCasaIniciadora());
            } else if (dados.getNomeCasaIniciadora() != null) {
                silver.setDetCasaIniciadora(dados.getNomeCasaIniciadora());
            }
        }

        var natureza = materia.getNaturezaMateria();
        if (natureza == null && dados != null) {
            natureza = dados.getNaturezaMateria();
        }
        if (natureza != null) {
            silver.setDetNaturezaCodigo(natureza.getCodigoNatureza());
            silver.setDetNaturezaNome(natureza.getNomeNatureza());
            silver.setDetNaturezaDescricao(natureza.getDescricaoNatureza());
        }

        var casaOrigem = materia.getCasaOrigem();
        if (casaOrigem == null) {
            casaOrigem = materia.getOrigemMateria();
        }
        if (casaOrigem != null) {
            silver.setDetSiglaCasaOrigem(casaOrigem.getSiglaCasaOrigem());
        }

        if (materia.getClassificacoes() != null && materia.getClassificacoes().getClassificacao() != null) {
            silver.setDetClassificacoes(writeJson(materia.getClassificacoes().getClassificacao()));
        }

        if (materia.getOutrasInformacoes() != null && materia.getOutrasInformacoes().getServico() != null) {
            silver.setDetOutrasInformacoes(writeJson(materia.getOutrasInformacoes().getServico()));
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[Enrich] Falha ao serializar JSON de detalhe do Senado: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<List<T>> partitionBySize(List<T> source, int partitions) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        int parts = Math.max(1, Math.min(partitions, source.size()));
        List<List<T>> result = new ArrayList<>(parts);
        int from = 0;
        for (int i = 0; i < parts; i++) {
            int remainingItems = source.size() - from;
            int remainingParts = parts - i;
            int chunkSize = (int) Math.ceil((double) remainingItems / remainingParts);
            int to = Math.min(source.size(), from + chunkSize);
            result.add(source.subList(from, to));
            from = to;
        }
        return result;
    }

    private int resolveSenadoEnrichmentParallelism() {
        if (senadoEnrichmentParallelism <= 0) {
            return 1;
        }
        return Math.min(senadoEnrichmentParallelism, 10);
    }

    private void throttle() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int resolveBatchSize() {
        return batchSize > 0 ? batchSize : 500;
    }

    // ─── Enriquecimento Senado Emendas ────────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com emendas via API /processo/emenda.
     *
     * @param materias lista de matérias a processar; usa todas com detalhe se null
     * @return total de novos registros de emenda inseridos
     */
    public int enriquecerEmendasSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de emendas", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoEmendaDTO> emendas = senadoApiExtractor.fetchEmendas(materia.getCodigo());
                    totalInseridos += emendaLoader.carregar(materia, emendas, materia.getEtlJobId());
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar emendas Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado emendas: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Enriquecimento Senado Documentos ─────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com documentos via API
     * /processo/documento.
     *
     * @param materias lista de matérias a processar; usa todas com detalhe se null
     * @return total de novos registros de documento inseridos
     */
    public int enriquecerDocumentosSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de documentos", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoDocumentoDTO> documentos = senadoApiExtractor.fetchDocumentos(materia.getCodigo());
                    totalInseridos += documentoLoader.carregar(materia, documentos, materia.getEtlJobId());
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar documentos Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado documentos: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Enriquecimento Senado Prazos ──────────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com prazos via API /processo/prazo.
     *
     * @param materias lista de matérias a processar; usa todas com detalhe se null
     * @return total de novos registros de prazo inseridos
     */
    public int enriquecerPrazosSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de prazos", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoPrazoDTO> prazos = senadoApiExtractor.fetchPrazos(materia.getCodigo());
                    totalInseridos += prazoLoader.carregar(materia, prazos, materia.getEtlJobId());
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar prazos Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado prazos: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Enriquecimento Senado Votações ──────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com votações via API.
     * Registros já existentes são ignorados (idempotente).
     *
     * @param materias lista de matérias a enriquecer; usa todas com detalhe se null
     * @return total de novos registros de votações inseridos
     */
    public int enriquecerVotacoesSenado(List<SilverSenadoMateria> materias) {
        List<SilverSenadoMateria> alvo = (materias != null)
                ? materias
                : silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} matérias Silver Senado para enriquecimento de votações", alvo.size());

        int totalInseridos = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            List<SilverSenadoMateria> lote = alvo.subList(i, fim);

            for (SilverSenadoMateria materia : lote) {
                if (materia.getCodigo() == null || materia.getCodigo().isBlank())
                    continue;
                try {
                    List<SenadoVotacaoDTO> votacoes = senadoApiExtractor.fetchVotacoes(materia.getCodigo());
                    totalInseridos += votacaoLoader.carregar(materia, votacoes, materia.getEtlJobId());
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar votações Senado codigo={}: {}",
                            materia.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado votações: {} novos registros para {} matérias",
                totalInseridos, alvo.size());
        return totalInseridos;
    }

    // ─── Enriquecimento Câmara Relacionadas ───────────────────────────────────

    /**
     * Enriquece registros Silver da Câmara com proposições relacionadas via API.
     * Registros já existentes são ignorados (idempotente).
     *
     * @param proposicoes lista de proposições Silver a enriquecer; usa todas se
     *                    null
     * @return total de novos registros de relacionadas inseridos
     */
    public int enriquecerRelacionadasCamara(java.util.List<SilverCamaraProposicao> proposicoes) {
        java.util.List<SilverCamaraProposicao> alvo = (proposicoes != null)
                ? proposicoes
                : silverCamaraRepository.findAll();
        int loteSize = resolveBatchSize();

        log.info("[Enrich] {} proposições Silver Câmara para enriquecimento de relacionadas", alvo.size());

        int totalInseridas = 0;
        for (int i = 0; i < alvo.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, alvo.size());
            java.util.List<SilverCamaraProposicao> lote = alvo.subList(i, fim);

            for (SilverCamaraProposicao prop : lote) {
                if (prop.getCamaraId() == null || prop.getCamaraId().isBlank())
                    continue;
                try {
                    java.util.List<br.leg.congresso.etl.extractor.camara.dto.CamaraRelacionadaDTO> relacionadas = camaraApiExtractor
                            .fetchRelacionadas(prop.getCamaraId());
                    totalInseridas += relacionadasLoader.carregar(prop, relacionadas, prop.getEtlJobId());
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao buscar relacionadas Câmara camaraId={}: {}",
                            prop.getCamaraId(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Câmara relacionadas: {} novas inseridas para {} proposições",
                totalInseridas, alvo.size());
        return totalInseridas;
    }
}
