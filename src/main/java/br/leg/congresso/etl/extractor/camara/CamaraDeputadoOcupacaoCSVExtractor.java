package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOcupacaoCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoOcupacaoLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai ocupações profissionais de deputados da Câmara a partir do CSV
 * deputadosOcupacoes.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/deputadosOcupacoes/csv/deputadosOcupacoes.csv
 * Arquivo não parametrizado por ano; usa ano=0 como chave interna de controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoOcupacaoCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/deputadosOcupacoes/csv/deputadosOcupacoes.csv";
    private static final String CSV_FILE_NAME = "deputadosOcupacoes.csv";
    private static final String DATASET_LABEL = "deputadosOcupacoes";
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoOcupacaoLoader loader;

    /**
     * Faz download do CSV e persiste todas as ocupações na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[DeputadosOcupacoes] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoOcupacaoCSVRow.class, chunk -> {
            List<CamaraDeputadoOcupacaoCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[DeputadosOcupacoes] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
