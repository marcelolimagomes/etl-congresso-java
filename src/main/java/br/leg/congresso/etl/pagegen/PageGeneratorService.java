package br.leg.congresso.etl.pagegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.pagegen.dto.ProposicaoIndexItemDTO;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de geração de páginas estáticas HTML para proposições.
 *
 * <p>
 * Fluxo:
 * <ol>
 * <li>Pagina as proposições Gold em lotes de {@code batchSize}</li>
 * <li>Para cada proposição, monta o DTO via
 * {@link ProposicaoPageAssembler}</li>
 * <li>Renderiza o HTML via {@link ThymeleafRenderer}</li>
 * <li>Grava em {@code outputDir/proposicoes/{casa}-{idOrigem}/index.html}</li>
 * </ol>
 *
 * <p>
 * Também delega para {@link SitemapGenerator} após a geração completa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageGeneratorService {

    private final ProposicaoRepository proposicaoRepository;
    private final ProposicaoPageAssembler assembler;
    private final ThymeleafRenderer renderer;
    private final SitemapGenerator sitemapGenerator;
    private final ObjectMapper objectMapper;

    @Value("${etl.pagegen.output-dir:open-data/public}")
    private String outputDirConfig;

    @Value("${etl.pagegen.batch-size:500}")
    private int batchSize;

    /** Número de itens por página no índice de proposições. */
    private static final int INDEX_PAGE_SIZE = 100;

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gera todas as páginas de proposições.
     *
     * @return número total de páginas geradas
     */
    @Transactional(readOnly = true)
    public int generateAll() {
        return generateAll(null);
    }

    /**
     * Gera páginas de proposições, opcionalmente filtrando por ano.
     *
     * @param ano se não nulo, restringe a geração ao ano informado
     * @return número total de páginas geradas
     */
    @Transactional(readOnly = true)
    public int generateAll(Integer ano) {
        Path base = resolveOutputDir();
        var counter = new AtomicInteger(0);
        var errors = new AtomicInteger(0);

        long total = (ano != null)
                ? proposicaoRepository.countByAno(ano)
                : proposicaoRepository.count();
        int pages = (int) Math.ceil((double) total / batchSize);
        log.info("Iniciando geração de páginas: {} proposições{}, {} lotes",
                total, (ano != null ? " (ano=" + ano + ")" : ""), pages);

        for (int page = 0; page < pages; page++) {
            var pageable = PageRequest.of(page, batchSize);
            var batch = (ano != null)
                    ? proposicaoRepository.findAllForPageGenByAno(ano, pageable)
                    : proposicaoRepository.findAllForPageGen(pageable);
            batch.forEach(proposicao -> {
                try {
                    var dto = assembler.assemble(proposicao);
                    var html = renderer.render("proposicao", Map.of("page", dto));
                    writeHtml(base, dto.getCasa(), dto.getIdOriginal(), html);
                    counter.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Falha ao gerar página para {} (id={}): {}",
                            proposicao.getSigla() + " " + proposicao.getNumero() + "/" + proposicao.getAno(),
                            proposicao.getId(), e.getMessage());
                    errors.incrementAndGet();
                }
            });
            log.info("Lote {}/{} concluído — geradas: {}, erros: {}", page + 1, pages, counter.get(), errors.get());
        }

        log.info("Geração concluída: {} páginas geradas, {} erros", counter.get(), errors.get());

        try {
            sitemapGenerator.generate(base);
        } catch (Exception e) {
            log.warn("Falha ao gerar sitemap: {}", e.getMessage());
        }

        try {
            generateIndex(base);
        } catch (Exception e) {
            log.warn("Falha ao gerar índice de proposições: {}", e.getMessage());
        }

        return counter.get();
    }

    /**
     * Gera o índice paginado de proposições como arquivos JSON em
     * {@code base/proposicoes/indice/data/page-{n}.json}.
     * As páginas HTML são renderizadas pelo Nuxt (SSG) consumindo esses arquivos.
     *
     * @param base diretório raiz do output (ex: {@code open-data/public})
     */
    @Transactional(readOnly = true)
    public void generateIndex(Path base) {
        long total = proposicaoRepository.count();
        int totalPages = (int) Math.ceil((double) total / INDEX_PAGE_SIZE);
        String geradoEm = LocalDateTime.now().format(DT_FMT);

        log.info("Gerando índice de proposições: {} itens, {} páginas", total, totalPages);

        for (int page = 0; page < totalPages; page++) {
            int pageNumber = page + 1; // 1-based
            var pageable = PageRequest.of(page, INDEX_PAGE_SIZE);
            var batch = proposicaoRepository.findAllForPageGen(pageable);

            List<ProposicaoIndexItemDTO> items = new ArrayList<>();
            for (var p : batch) {
                if (p.getIdOrigem() == null)
                    continue;
                String casa = p.getCasa().name().toLowerCase();
                String slugId = casa + "-" + p.getIdOrigem();
                String titulo = (p.getSigla() != null ? p.getSigla() : "")
                        + " " + (p.getNumero() != null ? p.getNumero() : "")
                        + "/" + (p.getAno() != null ? p.getAno() : "");
                String ementa = p.getEmenta() != null
                        ? (p.getEmenta().length() > 200 ? p.getEmenta().substring(0, 200) + "…" : p.getEmenta())
                        : "";
                String casaLabel = "camara".equals(casa) ? "Câmara dos Deputados" : "Senado Federal";
                String situacao = p.getSituacao() != null ? p.getSituacao() : "";
                String url = "/proposicoes/" + slugId;
                items.add(new ProposicaoIndexItemDTO(slugId, titulo.trim(), ementa, casaLabel, situacao, url));
            }

            try {
                var pageData = new HashMap<String, Object>();
                pageData.put("currentPage", pageNumber);
                pageData.put("totalPages", totalPages);
                pageData.put("totalItems", total);
                pageData.put("items", items);
                writeIndexJson(base, pageNumber, objectMapper.writeValueAsString(pageData));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao serializar JSON da página " + pageNumber, e);
            }
        }

        // Grava meta.json com informações gerais do índice
        if (totalPages > 0) {
            try {
                var meta = new HashMap<String, Object>();
                meta.put("totalPages", totalPages);
                meta.put("totalItems", total);
                meta.put("generatedAt", geradoEm);
                writeIndexMeta(base, objectMapper.writeValueAsString(meta));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao serializar meta.json do índice", e);
            }
        }

        log.info("Índice de proposições gerado: {} páginas", totalPages);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path resolveOutputDir() {
        Path base = Path.of(outputDirConfig);
        if (!base.isAbsolute()) {
            // relativo ao diretório de trabalho (raiz do projeto)
            base = Path.of(System.getProperty("user.dir")).resolve(base);
        }
        return base;
    }

    void writeHtml(Path base, String casa, String idOriginal, String html) {
        Path dir = base.resolve("proposicoes").resolve(casa + "-" + idOriginal);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("index.html"), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar HTML em " + dir, e);
        }
    }

    void writeIndexJson(Path base, int pageNumber, String json) {
        Path dir = base.resolve("proposicoes").resolve("indice").resolve("data");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("page-" + pageNumber + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar JSON do índice em " + dir, e);
        }
    }

    void writeIndexMeta(Path base, String json) {
        Path dir = base.resolve("proposicoes").resolve("indice").resolve("data");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("meta.json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar meta.json do índice em " + dir, e);
        }
    }
}
