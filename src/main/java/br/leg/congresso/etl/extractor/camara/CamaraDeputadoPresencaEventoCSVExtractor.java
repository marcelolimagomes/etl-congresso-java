package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoPresencaEventoCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoPresencaEventoLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai presenças de deputados em eventos da Câmara a partir do CSV
 * eventosPresencaDeputados-{ano}.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/eventosPresencaDeputados/csv/eventosPresencaDeputados-{ano}.csv
 * Arquivo parametrizado por ano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoPresencaEventoCSVExtractor {

    private static final String CSV_URL_TEMPLATE = "https://dadosabertos.camara.leg.br/arquivos/eventosPresencaDeputados/csv/eventosPresencaDeputados-%d.csv";
    private static final String CSV_FILE_NAME_TEMPLATE = "eventosPresencaDeputados-%d.csv";
    private static final String DATASET_LABEL = "eventosPresencaDeputados";

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoPresencaEventoLoader loader;

    /**
     * Faz download do CSV do ano informado e persiste presenças na camada Silver.
     *
     * @param ano        ano de referência
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(int ano, EtlJobControl jobControl) throws IOException {
        String csvUrl = String.format(CSV_URL_TEMPLATE, ano);
        String csvFileName = String.format(CSV_FILE_NAME_TEMPLATE, ano);
        log.info("[PresencaEventos] Iniciando extração CSV ({}): {}", ano, csvUrl);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                csvFileName, csvUrl, DATASET_LABEL, ano, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoPresencaEventoCSVRow.class, chunk -> {
            List<CamaraDeputadoPresencaEventoCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[PresencaEventos] Extração concluída ({}): {} linhas lidas", ano, total[0]);
        return total[0];
    }
}
