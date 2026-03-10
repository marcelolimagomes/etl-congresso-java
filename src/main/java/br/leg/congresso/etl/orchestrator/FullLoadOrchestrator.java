package br.leg.congresso.etl.orchestrator;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraFileDownloader;
import br.leg.congresso.etl.extractor.camara.CamaraFileDownloader.DownloadResult;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoAutorCSVRow;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoTemaCSVRow;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoCSVRow;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoOrientacaoCSVRow;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoVotoCSVRow;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.mapper.SenadoMateriaMapper;
import br.leg.congresso.etl.loader.silver.SilverCamaraAutoresLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraTemasLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraVotacoesLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraVotacoesOrientacoesLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraVotacoesVotosLoader;
import br.leg.congresso.etl.loader.silver.SilverEnrichmentService;
import br.leg.congresso.etl.loader.silver.SilverSenadoLoader;
import br.leg.congresso.etl.promoter.SilverToGoldService;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final SilverCamaraTemasLoader silverCamaraTemasLoader;
    private final SilverCamaraAutoresLoader silverCamaraAutoresLoader;
    private final SilverCamaraVotacoesLoader silverCamaraVotacoesLoader;
    private final SilverCamaraVotacoesOrientacoesLoader silverCamaraOrientacoesLoader;
    private final SilverCamaraVotacoesVotosLoader silverCamaraVotosLoader;
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

    @Value("${etl.camara.temas-csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/proposicoesTemas/csv/proposicoesTemas-{ano}.csv}")
    private String temasCsvUrlPattern;

    @Value("${etl.camara.temas-csv-file-pattern:proposicoesTemas-{ano}.csv}")
    private String temasCsvFilePattern;

    @Value("${etl.camara.autores-csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/proposicoesAutores/csv/proposicoesAutores-{ano}.csv}")
    private String autoresCsvUrlPattern;

    @Value("${etl.camara.autores-csv-file-pattern:proposicoesAutores-{ano}.csv}")
    private String autoresCsvFilePattern;

    @Value("${etl.camara.votacoes-csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/votacoes/csv/votacoes-{ano}.csv}")
    private String votacoesCsvUrlPattern;

    @Value("${etl.camara.votacoes-csv-file-pattern:votacoes-{ano}.csv}")
    private String votacoesCsvFilePattern;

    @Value("${etl.camara.orientacoes-csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/votacoesOrientacoes/csv/votacoesOrientacoes-{ano}.csv}")
    private String orientacoesCsvUrlPattern;

    @Value("${etl.camara.orientacoes-csv-file-pattern:votacoesOrientacoes-{ano}.csv}")
    private String orientacoesCsvFilePattern;

    @Value("${etl.camara.votos-csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/votacoesVotos/csv/votacoesVotos-{ano}.csv}")
    private String votosCsvUrlPattern;

    @Value("${etl.camara.votos-csv-file-pattern:votacoesVotos-{ano}.csv}")
    private String votosCsvFilePattern;

    /**
     * Executa a carga total da Câmara dos Deputados para os anos especificados.
     */
    public EtlJobControl executarCamara(int anoInicio, int anoFim) {
        log.info("Iniciando carga total Câmara: {} a {}", anoInicio, anoFim);

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.CAMARA,
                TipoExecucao.FULL,
                Map.of("anoInicio", anoInicio, "anoFim", anoFim));

        try {
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                log.info("Processando Câmara ano={}", ano);
                try {
                    DownloadResult download = camaraFileDownloader.downloadIfNeeded(ano, job);

                    if (!download.isNew()) {
                        log.info("Arquivo CSV ano={} não alterado. Reprocessando a partir do cache local.", ano);
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
                            "DOWNLOAD_ERROR", "CSV-" + ano, e.getMessage());
                    job.incrementarErros();
                }
            }

            int tramitacoesInseridas = silverEnrichmentService.enriquecerTramitacoesCamara(null);
            log.info("[Silver] Enriquecimento Câmara concluído: {} tramitações novas", tramitacoesInseridas);

            // Datasets suplementares: temas, autores, votações, orientações, votos
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                final int anoFinal = ano;
                processarDatasetCsvCamara(anoFinal, job, temasCsvFilePattern, temasCsvUrlPattern, "proposicoesTemas",
                        CamaraProposicaoTemaCSVRow.class,
                        rows -> silverCamaraTemasLoader.carregar(rows, job.getId()));
                processarDatasetCsvCamara(anoFinal, job, autoresCsvFilePattern, autoresCsvUrlPattern,
                        "proposicoesAutores",
                        CamaraProposicaoAutorCSVRow.class,
                        rows -> silverCamaraAutoresLoader.carregar(rows, job.getId()));
                processarDatasetCsvCamara(anoFinal, job, votacoesCsvFilePattern, votacoesCsvUrlPattern, "votacoes",
                        CamaraVotacaoCSVRow.class,
                        rows -> silverCamaraVotacoesLoader.carregar(rows, job.getId()));
                processarDatasetCsvCamara(anoFinal, job, orientacoesCsvFilePattern, orientacoesCsvUrlPattern,
                        "votacoesOrientacoes",
                        CamaraVotacaoOrientacaoCSVRow.class,
                        rows -> silverCamaraOrientacoesLoader.carregar(rows, job.getId()));
                processarDatasetCsvCamara(anoFinal, job, votosCsvFilePattern, votosCsvUrlPattern, "votacoesVotos",
                        CamaraVotacaoVotoCSVRow.class,
                        rows -> silverCamaraVotosLoader.carregar(rows, job.getId()));
            }

            int relacionadas = silverEnrichmentService.enriquecerRelacionadasCamara(null);
            log.info("[Silver] Relacionadas Câmara: {} novas", relacionadas);

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
                Map.of("anoInicio", anoInicio, "anoFim", anoFim));

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
            int autoriasInseridas = silverEnrichmentService.enriquecerAutoriasSenado(null);
            int relatoriasInseridas = silverEnrichmentService.enriquecerRelatoriasSenado(null);
            int emendasInseridas = silverEnrichmentService.enriquecerEmendasSenado(null);
            int documentosInseridos = silverEnrichmentService.enriquecerDocumentosSenado(null);
            int prazosInseridos = silverEnrichmentService.enriquecerPrazosSenado(null);
            int votacoesInseridas = silverEnrichmentService.enriquecerVotacoesSenado(null);
            log.info("[Silver] Enriquecimento Senado: {} detalhes, {} movim., {} autorias, {} relatorias, " +
                    "{} emendas, {} docs, {} prazos, {} votações",
                    detalhesEnriquecidos, movimentacoesInseridas, autoriasInseridas, relatoriasInseridas,
                    emendasInseridas, documentosInseridos, prazosInseridos, votacoesInseridas);

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
     * Baixa e processa um dataset CSV suplementar da Câmara para o ano informado.
     * Não lança exceção — erros são registrados no job e o processamento continua.
     */
    private <T> void processarDatasetCsvCamara(int ano, EtlJobControl job,
            String filePattern, String urlPattern, String datasetLabel,
            Class<T> rowType, Consumer<List<T>> loader) {
        try {
            DownloadResult dl = camaraFileDownloader.downloadDatasetIfNeeded(
                    filePattern, urlPattern, datasetLabel, ano, job);
            if (!dl.isNew()) {
                log.info("[Silver] Dataset {} ano={} não alterado. Reprocessando a partir do cache local.",
                        datasetLabel, ano);
            }
            camaraCSVExtractor.extractRowsInChunks(dl.filePath(), rowType, loader);
        } catch (IOException e) {
            log.error("Erro ao processar dataset {} ano={}: {}", datasetLabel, ano, e.getMessage());
            jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                    "DOWNLOAD_ERROR", datasetLabel + "-" + ano, e.getMessage());
            job.incrementarErros();
        } catch (Exception e) {
            log.error("Falha de processamento dataset {} ano={}: {}", datasetLabel, ano, e.getMessage(), e);
            jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                    "PROCESSING_ERROR", datasetLabel + "-" + ano, e.getMessage());
            job.incrementarErros();
        }
    }

    /**
     * Retorna o ano de início padrão para a Câmara.
     */
    public int getCamaraAnoInicio() {
        return camaraAnoInicio;
    }

    /**
     * Retorna o ano de início padrão para o Senado.
     */
    public int getSenadoAnoInicio() {
        return senadoAnoInicio;
    }

    /**
     * Retorna o ano atual.
     */
    public int getAnoAtual() {
        return LocalDate.now().getYear();
    }
}
