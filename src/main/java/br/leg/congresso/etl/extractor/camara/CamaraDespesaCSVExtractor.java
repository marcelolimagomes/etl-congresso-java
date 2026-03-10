package br.leg.congresso.etl.extractor.camara;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.opencsv.bean.CsvToBeanBuilder;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDespesaCSVRow;
import br.leg.congresso.etl.loader.silver.SilverCamaraDespesaLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai despesas CEAP da Câmara a partir do arquivo Ano-{ano}.csv.zip
 * e persiste na camada Silver.
 *
 * URL: http://www.camara.leg.br/cotas/Ano-{ano}.csv.zip
 * O arquivo está compactado em ZIP. A extração descomprime o CSV antes do
 * parse.
 * Encoding: detectado automaticamente (UTF-8 com BOM ou ISO-8859-1).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDespesaCSVExtractor {

    private static final String ZIP_URL_TEMPLATE = "http://www.camara.leg.br/cotas/Ano-%d.csv.zip";
    private static final String ZIP_FILE_TEMPLATE = "Ano-%d.csv.zip";
    private static final String CSV_FILE_TEMPLATE = "Ano-%d.csv";
    private static final String DATASET_LABEL = "ceap-despesas";

    private final CamaraFileDownloader fileDownloader;
    private final SilverCamaraDespesaLoader loader;

    @Value("${etl.camara.chunk-size:10000}")
    private int chunkSize;

    /**
     * Faz download do ZIP do ano informado, extrai o CSV e persiste na camada
     * Silver.
     *
     * @param ano        ano de referência
     * @param jobControl job ETL para rastreabilidade
     * @return total de linhas lidas
     */
    public long extrairEPersistir(int ano, EtlJobControl jobControl) throws IOException {
        String zipUrl = String.format(ZIP_URL_TEMPLATE, ano);
        String zipFileName = String.format(ZIP_FILE_TEMPLATE, ano);
        String csvFileName = String.format(CSV_FILE_TEMPLATE, ano);

        log.info("[CEAP] Iniciando extração ZIP ({}): {}", ano, zipUrl);

        CamaraFileDownloader.DownloadResult download = fileDownloader.downloadDatasetIfNeeded(
                zipFileName, zipUrl, DATASET_LABEL, ano, jobControl);

        Path zipPath = download.filePath();
        Path csvPath = zipPath.getParent().resolve(csvFileName);

        extrairCsvDoZip(zipPath, csvPath);
        log.info("[CEAP] CSV extraído do ZIP: {}", csvPath);

        try {
            long total = parsearEPersistir(csvPath, ano, jobControl);
            log.info("[CEAP] Extração concluída ({}): {} linhas lidas", ano, total);
            return total;
        } finally {
            Files.deleteIfExists(csvPath);
        }
    }

    // ── Extração do ZIP ────────────────────────────────────────────────────────

    private void extrairCsvDoZip(Path zipPath, Path destCsv) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
                    log.debug("[CEAP] Extraindo entrada ZIP: {}", entry.getName());
                    Files.copy(zis, destCsv, StandardCopyOption.REPLACE_EXISTING);
                    zis.closeEntry();
                    return;
                }
                zis.closeEntry();
            }
        }
        throw new IOException("Nenhuma entrada CSV encontrada no ZIP: " + zipPath);
    }

    // ── Parsing e carga ───────────────────────────────────────────────────────

    private long parsearEPersistir(Path csvPath, int ano, EtlJobControl jobControl) throws IOException {
        Charset charset = detectarCharset(csvPath);
        log.debug("[CEAP] Charset detectado para {} ({}): {}", csvPath.getFileName(), ano, charset.name());

        int effectiveChunkSize = chunkSize > 0 ? chunkSize : 10000;
        long totalLidas = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, charset)) {
            // Remove BOM se presente
            reader.mark(1);
            if (reader.read() != '\uFEFF') {
                reader.reset();
            }

            Iterator<CamaraDespesaCSVRow> it = new CsvToBeanBuilder<CamaraDespesaCSVRow>(reader)
                    .withType(CamaraDespesaCSVRow.class)
                    .withSeparator(';')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false)
                    .build()
                    .iterator();

            List<CamaraDespesaCSVRow> chunk = new ArrayList<>(effectiveChunkSize);
            while (it.hasNext()) {
                totalLidas++;
                chunk.add(it.next());
                if (chunk.size() >= effectiveChunkSize) {
                    loader.carregar(new ArrayList<>(chunk), jobControl.getId());
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                loader.carregar(chunk, jobControl.getId());
            }
        }

        return totalLidas;
    }

    /**
     * Detecta charset checando a presença de BOM UTF-8 (0xEF 0xBB 0xBF).
     * Retorna UTF-8 se BOM presente, ISO-8859-1 caso contrário.
     * ISO-8859-1 aceitará qualquer sequência de bytes sem lançar exceção.
     */
    private Charset detectarCharset(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] bom = new byte[3];
            int read = is.read(bom, 0, 3);
            if (read == 3
                    && (bom[0] & 0xFF) == 0xEF
                    && (bom[1] & 0xFF) == 0xBB
                    && (bom[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.ISO_8859_1;
    }
}
