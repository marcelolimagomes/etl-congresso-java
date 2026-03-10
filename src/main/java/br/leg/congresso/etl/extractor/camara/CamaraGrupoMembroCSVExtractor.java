package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraGrupoMembroCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraGrupoMembroLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai membros de grupos de trabalho da Câmara a partir do CSV
 * gruposMembros.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/gruposMembros/csv/gruposMembros.csv
 * Arquivo não parametrizado por ano; usa ano=0 como chave interna de controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraGrupoMembroCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/gruposMembros/csv/gruposMembros.csv";
    private static final String CSV_FILE_NAME = "gruposMembros.csv";
    private static final String DATASET_LABEL = "gruposMembros";
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraGrupoMembroLoader loader;

    /**
     * Faz download do CSV e persiste os membros de grupos na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[GruposMembros] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraGrupoMembroCSVRow.class, chunk -> {
            List<CamaraGrupoMembroCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[GruposMembros] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
