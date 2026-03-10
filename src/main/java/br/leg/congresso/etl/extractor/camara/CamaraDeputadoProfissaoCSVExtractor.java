package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoProfissaoCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoProfissaoLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai profissões de deputados da Câmara a partir do CSV
 * deputadosProfissoes.csv
 * e persiste na camada Silver via {@link SilverCamaraDeputadoProfissaoLoader}.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/deputadosProfissoes/csv/deputadosProfissoes.csv
 * Arquivo não parametrizado por ano; usa ano=0 como chave interna de controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoProfissaoCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/deputadosProfissoes/csv/deputadosProfissoes.csv";
    private static final String CSV_FILE_NAME = "deputadosProfissoes.csv";
    private static final String DATASET_LABEL = "deputadosProfissoes";
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoProfissaoLoader loader;

    /**
     * Faz download do CSV e persiste todas as profissões na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[DeputadosProfissoes] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoProfissaoCSVRow.class, chunk -> {
            List<CamaraDeputadoProfissaoCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[DeputadosProfissoes] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
