package br.leg.congresso.etl.orchestrator;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraFileDownloader;
import br.leg.congresso.etl.extractor.camara.CamaraFileDownloader.DownloadResult;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoCSVRow;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.mapper.SenadoMateriaMapper;
import br.leg.congresso.etl.loader.silver.SilverCamaraLoader;
import br.leg.congresso.etl.loader.silver.SilverEnrichmentService;
import br.leg.congresso.etl.loader.silver.SilverSenadoLoader;
import br.leg.congresso.etl.promoter.SilverToGoldService;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador de carga total (full load) para Câmara e Senado.
 * Baixa CSVs por ano para a Câmara e consulta a API REST para o Senado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FullLoadOrchestrator {

    private final CamaraFileDownloader camaraFileDownloader;
    private final CamaraCSVExtractor camaraCSVExtractor;
    private final CamaraProposicaoMapper camaraMapper;
    private final SenadoApiExtractor senadoApiExtractor;
    private final SenadoMateriaMapper senadoMapper;
    private final SilverCamaraLoader silverCamaraLoader;
    private final SilverSenadoLoader silverSenadoLoader;
    private final SilverEnrichmentService silverEnrichmentService;
    private final SilverToGoldService silverToGoldService;
    private final EtlJobControlService jobControlService;

    @Value("${etl.camara.ano-inicio:1988}")
    private int camaraAnoInicio;

    @Value("${etl.senado.ano-inicio:1988}")
    private int senadoAnoInicio;

    @Value("${etl.batch-size:500}")
    private int senadoBatchSize;

    /**
     * Executa a carga total da Câmara dos Deputados para os anos especificados.
     */
    public EtlJobControl executarCamara(int anoInicio, int anoFim) {
        log.info("Iniciando carga total Câmara: {} a {}", anoInicio, anoFim);

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.CAMARA,
            TipoExecucao.FULL,
            Map.of("anoInicio", anoInicio, "anoFim", anoFim)
        );

        try {
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                log.info("Processando Câmara ano={}", ano);
                try {
                    DownloadResult download = camaraFileDownloader.downloadIfNeeded(ano, job);

                    if (!download.isNew()) {
                        log.info("Arquivo CSV ano={} não alterado. Pulando.", ano);
                        continue;
                    }

                    // Carga Silver: passthrough fiel à fonte (um passe sobre o CSV)
                    camaraCSVExtractor.extractRawInChunks(download.filePath(), rows -> {
                        List<SilverCamaraProposicao> silverLote = rows.stream()
                            .map(camaraMapper::csvRowToSilver)
                            .toList();
                        silverCamaraLoader.carregar(silverLote, job);
                    });

                } catch (IOException e) {
                    log.error("Erro ao baixar/processar CSV Câmara ano={}: {}", ano, e.getMessage());
                    jobControlService.registrarErro(
                        job, CasaLegislativa.CAMARA,
                        "DOWNLOAD_ERROR", "CSV-" + ano, e.getMessage()
                    );
                    job.incrementarErros();
                }
            }

            int tramitacoesInseridas = silverEnrichmentService.enriquecerTramitacoesCamara(null);
            log.info("[Silver] Enriquecimento Câmara concluído: {} tramitações novas", tramitacoesInseridas);

            // Gold é alimentado exclusivamente via promoção Silver → Gold
            silverToGoldService.promoverCamara();

            return jobControlService.finalizar(job);

        } catch (Exception e) {
            log.error("Falha crítica na carga total Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Executa a carga total do Senado Federal para os anos especificados.
     */
    public EtlJobControl executarSenado(int anoInicio, int anoFim) {
        log.info("Iniciando carga total Senado: {} a {}", anoInicio, anoFim);

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.SENADO,
            TipoExecucao.FULL,
            Map.of("anoInicio", anoInicio, "anoFim", anoFim)
        );

        try {
            // Carga Silver Senado: passthrough fiel à fonte
            List<SenadoMateriaDTO.Materia> materiasRaw = senadoApiExtractor.extractRawByYearRange(anoInicio, anoFim);
            log.info("[Silver] Senado: {} matérias brutas. Iniciando carga Silver.", materiasRaw.size());

            for (int i = 0; i < materiasRaw.size(); i += senadoBatchSize) {
                int fim = Math.min(i + senadoBatchSize, materiasRaw.size());
                List<SilverSenadoMateria> silverLote = materiasRaw.subList(i, fim).stream()
                    .map(senadoMapper::materiaToSilver)
                    .toList();
                silverSenadoLoader.carregar(silverLote, job);
            }

            int detalhesEnriquecidos = silverEnrichmentService.enriquecerDetalhesSenado();
            int movimentacoesInseridas = silverEnrichmentService.enriquecerMovimentacoesSenado(null);
            log.info("[Silver] Enriquecimento Senado concluído: {} detalhes e {} movimentações novas",
                detalhesEnriquecidos, movimentacoesInseridas);

            // Gold é alimentado exclusivamente via promoção Silver → Gold
            silverToGoldService.promoverSenado();

            return jobControlService.finalizar(job);

        } catch (Exception e) {
            log.error("Falha crítica na carga total Senado: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Retorna o ano de início padrão para a Câmara.
     */
    public int getCamaraAnoInicio() { return camaraAnoInicio; }

    /**
     * Retorna o ano de início padrão para o Senado.
     */
    public int getSenadoAnoInicio() { return senadoAnoInicio; }

    /**
     * Retorna o ano atual.
     */
    public int getAnoAtual() { return LocalDate.now().getYear(); }
}
