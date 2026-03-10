package br.leg.congresso.etl.extractor.camara;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai deputados da Câmara a partir do arquivo CSV deputados.csv e persiste
 * na camada Silver via {@link SilverCamaraDeputadoLoader}.
 *
 * URL do arquivo:
 * https://dadosabertos.camara.leg.br/arquivos/deputados/csv/deputados.csv
 *
 * Princípio Silver: passthrough fiel à fonte — sem transformações.
 * O arquivo não é parametrizado por ano; usa ano=0 como chave interna de
 * controle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoCSVExtractor {

    private static final String CSV_URL = "https://dadosabertos.camara.leg.br/arquivos/deputados/csv/deputados.csv";
    private static final String CSV_FILE_NAME = "deputados.csv";
    private static final String DATASET_LABEL = "deputados";
    /** Convenção: ano=0 para datasets sem partição por ano */
    private static final int ANO_FIXO = 0;

    private final CamaraFileDownloader fileDownloader;
    private final CamaraCSVExtractor csvExtractor;
    private final SilverCamaraDeputadoLoader loader;

    /**
     * Faz download do CSV (com deduplicação por checksum) e persiste todos os
     * deputados na camada Silver.
     *
     * @param jobControl job ETL para rastreabilidade e contadores
     * @return total de linhas processadas
     */
    public long extrairEPersistir(EtlJobControl jobControl) throws IOException {
        log.info("[Deputados] Iniciando extração CSV: {}", CSV_URL);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                CSV_FILE_NAME, CSV_URL, DATASET_LABEL, ANO_FIXO, jobControl);

        Path csvPath = download.filePath();

        long[] total = { 0 };
        total[0] = csvExtractor.extractRowsInChunks(csvPath, CamaraDeputadoCSVRow.class, chunk -> {
            List<SilverCamaraDeputado> silver = chunk.stream()
                    .map(this::toSilver)
                    .toList();
            loader.carregar(silver, jobControl);
        });

        log.info("[Deputados] Extração concluída: {} linhas lidas", total[0]);
        return total[0];
    }

    private SilverCamaraDeputado toSilver(CamaraDeputadoCSVRow row) {
        return SilverCamaraDeputado.builder()
                .camaraId(extractIdFromUri(row.getUri()))
                .uri(row.getUri())
                .nomeCivil(row.getNomeCivil())
                .nomeParlamentar(row.getNome())
                .sexo(row.getSiglaSexo())
                .dataNascimento(row.getDataNascimento())
                .dataFalecimento(row.getDataFalecimento())
                .ufNascimento(row.getUfNascimento())
                .municipioNascimento(row.getMunicipioNascimento())
                .cpf(row.getCpf())
                .urlWebsite(row.getUrlWebsite())
                .primeiraLegislatura(row.getIdLegislaturaInicial())
                .ultimaLegislatura(row.getIdLegislaturaFinal())
                .build();
    }

    private String extractIdFromUri(String uri) {
        if (uri == null || uri.isBlank())
            return null;
        int lastSlash = uri.lastIndexOf('/');
        return (lastSlash >= 0 && lastSlash < uri.length() - 1) ? uri.substring(lastSlash + 1) : null;
    }
}
