package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoFrenteCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoFrenteLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai participação de deputados em frentes parlamentares a partir do CSV
 * frentesDeputados.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/frentesDeputados/csv/frentesDeputados.csv
 * Arquivo não parametrizado por ano; usa ano=0 como chave interna de controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoFrenteCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/frentesDeputados/csv/frentesDeputados.csv";
    private static final String CSV_FILE_NAME = "frentesDeputados.csv";
    private static final String DATASET_LABEL = "frentesDeputados";
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoFrenteLoader loader;

    /**
     * Faz download do CSV e persiste todas as frentes na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[FrentesDeputados] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoFrenteCSVRow.class, chunk -> {
            List<CamaraDeputadoFrenteCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[FrentesDeputados] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
