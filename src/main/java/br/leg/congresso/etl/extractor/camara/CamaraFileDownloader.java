package br.leg.congresso.etl.extractor.camara;

import br.leg.congresso.etl.domain.EtlFileControl;
import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import br.leg.congresso.etl.repository.EtlFileControlRepository;
import br.leg.congresso.etl.transformer.ContentHashGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Respons\u00e1vel por baixar os arquivos CSV de proposi\u00e7\u00f5es da C\u00e2mara dos Deputados.
 * Faz download streaming direto para disco — sem limite de buffer em mem\u00f3ria.
 * Gerencia o controle de arquivos para evitar re-downloads desnecess\u00e1rios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraFileDownloader {

    @Qualifier("camaraCsvWebClient")
    private final WebClient csvWebClient;  // mantido para futuras opera\u00e7\u00f5es reactivas
    private final EtlFileControlRepository fileControlRepository;
    private final ContentHashGenerator hashGenerator;

    @Value("${etl.camara.csv-url-pattern:https://dadosabertos.camara.leg.br/arquivos/proposicoes/csv/proposicoes-{ano}.csv}")
    private String csvUrlPattern;

    @Value("${etl.camara.csv-tmp-dir:/tmp/etl/camara}")
    private String tmpDir;

    /** HttpClient nativo Java 11+ — segue redirects e n\u00e3o tem limite de buffer. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    /**
     * Resultado do download de um arquivo CSV.
     */
    public record DownloadResult(
            Path filePath,
            String checksum,
            boolean isNew,
            EtlFileControl fileControl
    ) {}

    /**
     * Baixa o arquivo CSV para o ano informado, se necess\u00e1rio.
     * Retorna o resultado com o caminho do arquivo e informa\u00e7\u00f5es de controle.
     */
    public DownloadResult downloadIfNeeded(int ano, EtlJobControl jobControl) throws IOException {
        String fileName  = "proposicoes-" + ano + ".csv";
        String url       = csvUrlPattern.replace("{ano}", String.valueOf(ano));
        Path   destDir   = Paths.get(tmpDir, String.valueOf(ano));
        Path   destFile  = destDir.resolve(fileName);

        Files.createDirectories(destDir);
        log.info("Verificando arquivo CSV: ano={}, url={}", ano, url);

        Optional<EtlFileControl> existingControl = fileControlRepository
                .findByOrigemAndNomeArquivo(CasaLegislativa.CAMARA, fileName);

        // Download streaming direto para arquivo tempor\u00e1rio
        Path tmpFile = destDir.resolve(fileName + ".tmp");
        long bytesDownloaded = streamToFile(url, tmpFile);
        log.info("Download conclu\u00eddo: {} bytes -> {}", bytesDownloaded, tmpFile);

        // Calcula checksum do arquivo em disco (sem carregar em mem\u00f3ria)
        String novoChecksum = hashGenerator.generateForFilePath(tmpFile);

        if (existingControl.isPresent()) {
            EtlFileControl ctrl = existingControl.get();
            if (!ctrl.deveProcessar(novoChecksum)) {
                log.info("Arquivo CSV ano={} n\u00e3o foi alterado (checksum id\u00eantico). Pulando.", ano);
                Files.deleteIfExists(tmpFile);
                return new DownloadResult(destFile, novoChecksum, false, ctrl);
            }
            log.info("Arquivo CSV ano={} foi alterado. Reprocessando.", ano);
        }

        // Move para destino final
        Files.move(tmpFile, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Arquivo CSV salvo: {} ({} bytes)", destFile, bytesDownloaded);

        // Atualiza/cria controle de arquivo
        EtlFileControl fileControl = existingControl.orElseGet(() -> {
            EtlFileControl novo = new EtlFileControl();
            novo.setOrigem(CasaLegislativa.CAMARA);
            novo.setNomeArquivo(fileName);
            novo.setAnoReferencia(ano);
            novo.setUrlDownload(url);
            return novo;
        });

        fileControl.setChecksumSha256(novoChecksum);
        fileControl.setTamanhoBytes(bytesDownloaded);
        fileControl.setProcessadoEm(LocalDateTime.now());
        fileControl.setStatus(StatusEtl.SUCCESS);
        fileControl.setJob(jobControl);
        fileControlRepository.save(fileControl);

        return new DownloadResult(destFile, novoChecksum, true, fileControl);
    }

    /**
     * Download streaming para arquivo usando java.net.http.HttpClient.
     * N\u00e3o carrega o conte\u00fado em mem\u00f3ria — ideal para arquivos grandes (> 50 MB).
     */
    private long streamToFile(String url, Path dest) throws IOException {
        log.debug("Streaming CSV para disco: {} -> {}", url, dest);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "etl-congresso/1.0 (dados-abertos)")
                    .GET()
                    .build();
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(dest));
            if (response.statusCode() != 200) {
                throw new IOException("Download falhou com status " + response.statusCode() + " para: " + url);
            }
            return Files.size(dest);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrompido: " + url, e);
        }
    }
}
