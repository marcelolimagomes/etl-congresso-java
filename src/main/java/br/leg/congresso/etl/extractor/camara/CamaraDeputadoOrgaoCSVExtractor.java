package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOrgaoCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoOrgaoLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai participação de deputados em órgãos da Câmara a partir do CSV
 * orgaosDeputados-L{leg}.csv e persiste na camada Silver.
 *
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/orgaosDeputados/csv/orgaosDeputados-L{leg}.csv
 * Parametrizado pela legislatura atual (padrão: 57).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoOrgaoCSVExtractor {

    private static final String CSV_URL_TEMPLATE = "https://dadosabertos.camara.leg.br/arquivos/orgaosDeputados/csv/orgaosDeputados-L%d.csv";
    private static final String DATASET_LABEL = "orgaosDeputados";
    /** Convenção: ano=0 para datasets sem partição por ano */
    private static final int ANO_FIXO = 0;

    @Value("${etl.camara.legislatura-atual:57}")
    private int legislaturaAtual;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoOrgaoLoader loader;

    /**
     * Faz download do CSV da legislatura atual e persiste os órgãos na camada
     * Silver.
     *
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        String csvUrl = String.format(CSV_URL_TEMPLATE, legislaturaAtual);
        String csvFileName = String.format("orgaosDeputados-L%d.csv", legislaturaAtual);
        log.info("[OrgaosDeputados] Iniciando extração CSV (L{}): {}", legislaturaAtual, csvUrl);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                csvFileName, csvUrl, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();
        long[] total = { 0 };

        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoOrgaoCSVRow.class, chunk -> {
            List<CamaraDeputadoOrgaoCSVRow> rows = chunk;
            loader.carregar(rows, jobControl.getId());
        });

        log.info("[OrgaosDeputados] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }
}
