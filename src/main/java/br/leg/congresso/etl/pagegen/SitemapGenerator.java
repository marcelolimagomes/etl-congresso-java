package br.leg.congresso.etl.pagegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import br.leg.congresso.etl.repository.ProposicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gera o arquivo {@code sitemap-proposicoes.xml} em {@code base/}.
 *
 * <p>
 * Formato: Sitemap 0.9, UTF-8. Cada URL aponta para
 * {@code /proposicoes/{casa}-{idOrigem}} com {@code changefreq=weekly}
 * e {@code priority} variando entre camara (0.7) e senado (0.6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapGenerator {

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final int BATCH = 1000;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ProposicaoRepository proposicaoRepository;

    /**
     * Gera {@code base/sitemap-proposicoes.xml}.
     *
     * @param base diretório raiz do output (ex: {@code open-data/public})
     */
    public void generate(Path base) {
        log.info("Gerando sitemap de proposições em {}", base);
        var sb = new StringBuilder(4_000_000);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        String today = LocalDateTime.now().format(ISO_DATE);
        long total = proposicaoRepository.count();
        int pages = (int) Math.ceil((double) total / BATCH);

        for (int p = 0; p < pages; p++) {
            List<ProposicaoRepository.SitemapProjection> batch = proposicaoRepository
                    .findAllForSitemap(PageRequest.of(p, BATCH)).getContent();
            for (var prop : batch) {
                if (prop.getIdOrigem() == null)
                    continue;
                String casa = prop.getCasa().name().toLowerCase();
                String priority = "camara".equals(casa) ? "0.7" : "0.6";
                String lastmod = (prop.getAtualizadoEm() != null)
                        ? prop.getAtualizadoEm().format(ISO_DATE)
                        : today;

                sb.append("  <url>\n");
                sb.append("    <loc>").append(BASE_URL).append("/proposicoes/")
                        .append(casa).append("-").append(prop.getIdOrigem())
                        .append("</loc>\n");
                sb.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
                sb.append("    <changefreq>weekly</changefreq>\n");
                sb.append("    <priority>").append(priority).append("</priority>\n");
                sb.append("  </url>\n");
            }
        }

        sb.append("</urlset>\n");

        Path sitemapFile = base.resolve("sitemap-proposicoes.xml");
        try {
            Files.createDirectories(base);
            Files.writeString(sitemapFile, sb.toString(), StandardCharsets.UTF_8);
            log.info("Sitemap gerado: {} ({} bytes)", sitemapFile, sb.length());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar sitemap em " + sitemapFile, e);
        }
    }
}
