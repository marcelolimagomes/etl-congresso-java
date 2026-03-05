package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraApiExtractor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Serviço de enriquecimento Silver.
 *
 * Após a carga inicial dos registros Silver (passthrough da fonte principal),
 * este serviço consulta endpoints secundários para preencher:
 *
 * <ul>
 *   <li>Senado detalhe: campos {@code det_*} em {@code silver.senado_materia}</li>
 *   <li>Câmara tramitações: registros em {@code silver.camara_tramitacao}</li>
 *   <li>Senado movimentações: registros em {@code silver.senado_movimentacao}</li>
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
    private final EtlJobControlService jobControlService;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    // ─── Enriquecimento Senado Detalhe ─────────────────────────────────────────

    /**
     * Enriquece registros Silver do Senado com dados do endpoint de detalhe.
     * Apenas registros com {@code det_sigla_casa_identificacao IS NULL} são processados.
     *
     * @return número de registros efetivamente enriquecidos
     */
    public int enriquecerDetalhesSenado() {
        List<SilverSenadoMateria> pendentes = silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull();
        log.info("[Enrich] {} registros Silver Senado pendentes de enriquecimento de detalhe", pendentes.size());
        int loteSize = resolveBatchSize();

        int enriquecidos = 0;
        for (int i = 0; i < pendentes.size(); i += loteSize) {
            int fim = Math.min(i + loteSize, pendentes.size());
            List<SilverSenadoMateria> lote = pendentes.subList(i, fim);

            for (SilverSenadoMateria silver : lote) {
                try {
                    SenadoDetalheDTO detalhe = senadoApiExtractor.fetchRawDetalhe(silver.getCodigo());
                    if (detalhe != null) {
                        applyDetalhe(silver, detalhe);
                        silverSenadoRepository.save(silver);
                        enriquecidos++;
                    }
                    throttle();
                } catch (Exception e) {
                    log.warn("[Enrich] Falha ao enriquecer Senado código={}: {}", silver.getCodigo(), e.getMessage());
                }
            }
        }

        log.info("[Enrich] Senado detalhe concluído: {}/{} registros enriquecidos", enriquecidos, pendentes.size());
        return enriquecidos;
    }

    // ─── Enriquecimento Câmara Tramitações ────────────────────────────────────

    /**
     * Enriquece registros Silver da Câmara com tramitações do endpoint REST.
     * Tramitações já existentes são ignoradas (idempotente).
     *
     * @param proposicoes lista de proposições Silver a enriquecer; usa todas se null
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
                if (prop.getCamaraId() == null || prop.getCamaraId().isBlank()) continue;
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
                if (materia.getCodigo() == null || materia.getCodigo().isBlank()) continue;
                try {
                    List<SenadoMovimentacaoDTO.Movimentacao> movs = senadoApiExtractor.fetchRawMovimentacoes(materia.getCodigo());
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
            Map.of("operacao", "silver_enrichment", "escopo", "ambas_casas")
        );

        try {
            int detalhes = enriquecerDetalhesSenado();
            int tramitacoes = enriquecerTramitacoesCamara(null);
            int movimentacoes = enriquecerMovimentacoesSenado(null);

            int totalInserido = detalhes + tramitacoes + movimentacoes;
            for (int i = 0; i < totalInserido; i++) {
                job.incrementarInserido();
                job.incrementarProcessado();
            }

            log.info("[Enrich] Concluído: {} detalhes Senado, {} tramitações Câmara, {} movimentações Senado",
                detalhes, tramitacoes, movimentacoes);

            return jobControlService.finalizar(job);
        } catch (Exception e) {
            log.error("[Enrich] Falha crítica no enriquecimento Silver: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Aplica os campos do endpoint de detalhe no registro Silver.
     * Passthrough: sem normalização de nenhum campo.
     */
    public void applyDetalhe(SilverSenadoMateria silver, SenadoDetalheDTO detalhe) {
        if (detalhe == null || detalhe.getDetalheMateria() == null
                || detalhe.getDetalheMateria().getMateria() == null) return;

        var materia = detalhe.getDetalheMateria().getMateria();

        var dados = materia.getDadosBasicosMateria();
        if (dados != null) {
            silver.setDetSiglaCasaIdentificacao(dados.getSiglaCasaIdentificacao());
            silver.setDetSiglaSubtipo(dados.getSiglaSubtipo());
            silver.setDetDescricaoSubtipo(dados.getDescricaoSubtipo());
            silver.setDetDescricaoObjetivoProcesso(dados.getDescricaoObjetivoProcesso());
            silver.setDetIndicadorTramitando(dados.getIndicadorTramitando());
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
        if (natureza != null) {
            silver.setDetNaturezaCodigo(natureza.getCodigoNatureza());
            silver.setDetNaturezaNome(natureza.getNomeNatureza());
            silver.setDetNaturezaDescricao(natureza.getDescricaoNatureza());
        }

        var casaOrigem = materia.getCasaOrigem();
        if (casaOrigem != null) {
            silver.setDetSiglaCasaOrigem(casaOrigem.getSiglaCasaOrigem());
        }
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
}
