package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraMesaDiretoraCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraMesaDiretoraLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai a composição da Mesa Diretora da Câmara a partir do CSV
 * legislaturasMesas.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/legislaturasMesas/csv/legislaturasMesas.csv
 * Arquivo não parametrizado por ano; usa ano=0 como chave interna de controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraMesaDiretoraCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/legislaturasMesas/csv/legislaturasMesas.csv";
    private static final String CSV_FILE_NAME = "legislaturasMesas.csv";
    private static final String DATASET_LABEL = "legislaturasMesas";
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraMesaDiretoraLoader loader;

    /**
     * Faz download do CSV e persiste a Mesa Diretora na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[LegislaturasMesas] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraMesaDiretoraCSVRow.class, chunk -> {
            List<CamaraMesaDiretoraCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[LegislaturasMesas] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
