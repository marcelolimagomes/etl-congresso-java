package br.leg.congresso.etl.pagegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParlamentaresSitemapGenerator {

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final int BATCH = 1000;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SilverCamaraDeputadoRepository deputadoRepository;
    private final SilverSenadoSenadorRepository senadorRepository;

    public void generate(Path base) {
        long totalDeputados = deputadoRepository.count();
        long totalSenadores = senadorRepository.count();
        long total = totalDeputados + totalSenadores;

        if (total == 0) {
            log.info("Nenhum parlamentar encontrado para geração do sitemap.");
            return;
        }

        log.info("Gerando sitemap de parlamentares em {}", base);
        String today = LocalDateTime.now().format(ISO_DATE);
        var sb = new StringBuilder(256_000);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendDeputados(sb, today, totalDeputados);
        appendSenadores(sb, today, totalSenadores);

        sb.append("</urlset>\n");

        Path sitemapFile = base.resolve("sitemap-parlamentares.xml");
        try {
            Files.createDirectories(base);
            Files.writeString(sitemapFile, sb.toString(), StandardCharsets.UTF_8);
            log.info("Sitemap de parlamentares gerado: {} ({} bytes)", sitemapFile, sb.length());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar sitemap em " + sitemapFile, e);
        }
    }

    private void appendDeputados(StringBuilder sb, String today, long totalDeputados) {
        int pages = (int) Math.ceil((double) totalDeputados / BATCH);
        for (int page = 0; page < pages; page++) {
            for (SilverCamaraDeputado deputado : deputadoRepository.findAll(PageRequest.of(page, BATCH)).getContent()) {
                if (deputado.getCamaraId() == null || deputado.getCamaraId().isBlank()) {
                    continue;
                }
                appendUrl(
                        sb,
                        BASE_URL + "/" + ParlamentaresPageGeneratorService.buildDeputadoOutputPath(deputado.getCamaraId()) + "/",
                        formatDate(deputado.getAtualizadoEm(), deputado.getIngeridoEm(), today),
                        "weekly",
                        "0.7");
            }
        }
    }

    private void appendSenadores(StringBuilder sb, String today, long totalSenadores) {
        int pages = (int) Math.ceil((double) totalSenadores / BATCH);
        for (int page = 0; page < pages; page++) {
            for (SilverSenadoSenador senador : senadorRepository.findAll(PageRequest.of(page, BATCH)).getContent()) {
                if (senador.getCodigoSenador() == null || senador.getCodigoSenador().isBlank()) {
                    continue;
                }
                appendUrl(
                        sb,
                        BASE_URL + "/" + ParlamentaresPageGeneratorService.buildSenadorOutputPath(senador.getCodigoSenador()) + "/",
                        formatDate(senador.getAtualizadoEm(), senador.getIngeridoEm(), today),
                        "weekly",
                        "0.6");
            }
        }
    }

    private String formatDate(LocalDateTime atualizadoEm, LocalDateTime ingeridoEm, String fallback) {
        LocalDateTime reference = atualizadoEm != null ? atualizadoEm : ingeridoEm;
        return reference != null ? reference.format(ISO_DATE) : fallback;
    }

    private void appendUrl(StringBuilder sb, String loc, String lastmod, String changefreq, String priority) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(loc).append("</loc>\n");
        sb.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }
}