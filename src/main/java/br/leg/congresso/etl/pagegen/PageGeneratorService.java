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

        try {
            generateStaticIndex(base);
        } catch (Exception e) {
            log.warn("Falha ao gerar índice estático HTML de proposições: {}", e.getMessage());
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
        Path dir = base.resolve("stat-proposicoes").resolve(casa + "-" + idOriginal);
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

    // ── Índice estático HTML ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    void generateStaticIndex(Path base) {
        long total = proposicaoRepository.count();
        if (total == 0) {
            return;
        }
        int pages = (int) Math.ceil((double) total / batchSize);
        List<ProposicaoIndexItemDTO> allItems = new ArrayList<>((int) total);
        for (int page = 0; page < pages; page++) {
            var pageable = PageRequest.of(page, batchSize);
            var batch = proposicaoRepository.findAllForPageGen(pageable);
            for (var p : batch) {
                if (p.getIdOrigem() == null) continue;
                String casa = p.getCasa().name().toLowerCase();
                String slugId = casa + "-" + p.getIdOrigem();
                String titulo = ((p.getSigla() != null ? p.getSigla() : "") + " "
                        + (p.getNumero() != null ? p.getNumero() : "")
                        + "/" + (p.getAno() != null ? p.getAno() : "")).trim();
                String ementa = p.getEmenta() != null
                        ? (p.getEmenta().length() > 200 ? p.getEmenta().substring(0, 200) + "\u2026" : p.getEmenta())
                        : "";
                String casaLabel = "camara".equals(casa) ? "C\u00e2mara dos Deputados" : "Senado Federal";
                String situacao = p.getSituacao() != null ? p.getSituacao() : "";
                String url = "/stat-proposicoes/" + slugId + "/";
                allItems.add(new ProposicaoIndexItemDTO(slugId, titulo, ementa, casaLabel, situacao, url));
            }
        }
        String generatedAt = LocalDateTime.now().format(DT_FMT);
        String html = buildProposicoesIndexHtml(allItems, generatedAt);
        Path indexFile = base.resolve("stat-proposicoes").resolve("index.html");
        try {
            Files.createDirectories(indexFile.getParent());
            Files.writeString(indexFile, html, StandardCharsets.UTF_8);
            log.info("Índice estático de proposições gravado: {} ({} itens)", indexFile, allItems.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar índice estático de proposições", e);
        }
    }

    private String buildProposicoesIndexHtml(List<ProposicaoIndexItemDTO> items, String generatedAt) {
        int total = items.size();
        long totalCamara = items.stream().filter(i -> i.casaLabel().contains("C\u00e2mara")).count();
        long totalSenado = total - totalCamara;
        var sb = new StringBuilder(total * 500 + 12000);
        sb.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\" />\n")
          .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n")
          .append("  <title>\u00cdndice de Proposi\u00e7\u00f5es Legislativas \u2014 Transpar\u00eancia Legislativa</title>\n")
          .append("  <meta name=\"description\" content=\"Lista completa de ").append(total)
          .append(" proposi\u00e7\u00f5es do Congresso Nacional.\" />\n")
          .append("  <link rel=\"canonical\" href=\"https://www.translegis.com.br/stat-proposicoes/\" />\n")
          .append("  <script>(function(){try{var m=localStorage.getItem('nuxt-color-mode')||'light';")
          .append("if(m==='dark'||(m==='system'&&window.matchMedia('(prefers-color-scheme:dark)').matches))")
          .append("{document.documentElement.classList.add('dark');}}catch(e){}}());</script>\n")
          .append("  <style>\n")
          .append("    :root{--bg:#f9fafb;--bgc:#fff;--tx:#111827;--txm:#6b7280;--bd:#e5e7eb;--hd:#1a4480;--lk:#3b82f6;--sp:#f3f4f6;}\n")
          .append("    :root.dark{--bg:#111827;--bgc:#1f2937;--tx:#f1f5f9;--txm:#9ca3af;--bd:#374151;--hd:#60a5fa;--lk:#93c5fd;--sp:#374151;}\n")
          .append("    *{box-sizing:border-box;margin:0;padding:0;}\n")
          .append("    body{font-family:system-ui,-apple-system,sans-serif;background:var(--bg);color:var(--tx);line-height:1.5;}\n")
          .append("    .container{max-width:1100px;margin:0 auto;padding:2rem 1rem;}\n")
          .append("    h1{color:var(--hd);font-size:1.5rem;margin:.25rem 0 .5rem;}\n")
          .append("    a{color:var(--lk);text-decoration:none;}a:hover{text-decoration:underline;}\n")
          .append("    .bc{font-size:.875rem;color:var(--txm);margin-bottom:1.25rem;}.bc a{color:var(--lk);}\n")
          .append("    .stats{font-size:.875rem;color:var(--txm);margin-bottom:1.5rem;}\n")
          .append("    .srch{margin:1rem 0 1.5rem;}.srch input{width:100%;padding:.625rem 1rem;border:1px solid var(--bd);border-radius:.5rem;background:var(--bgc);color:var(--tx);font-size:1rem;}\n")
          .append("    .srch input:focus{outline:2px solid var(--lk);border-color:transparent;}.srch input::placeholder{color:var(--txm);}\n")
          .append("    #list{border:1px solid var(--bd);border-radius:.5rem;overflow:hidden;}\n")
          .append("    .item{padding:.75rem 1rem;border-bottom:1px solid var(--bd);background:var(--bgc);}\n")
          .append("    .item:last-child{border-bottom:none;}\n")
          .append("    .it{font-weight:600;color:var(--tx);}\n")
          .append("    .em{font-size:.85rem;color:var(--txm);margin:.25rem 0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}\n")
          .append("    .mt{display:flex;gap:.75rem;align-items:center;margin-top:.25rem;font-size:.8rem;color:var(--txm);}\n")
          .append("    .badge{display:inline-block;padding:.15rem .5rem;border-radius:9999px;font-size:.7rem;font-weight:600;}\n")
          .append("    .bc-c{background:#dcfce7;color:#166534;}.bc-s{background:#dbeafe;color:#1e40af;}\n")
          .append("    :root.dark .bc-c{background:#14532d;color:#86efac;}:root.dark .bc-s{background:#1e3a5f;color:#93c5fd;}\n")
          .append("    #nr{display:none;padding:2rem;text-align:center;color:var(--txm);}\n")
          .append("    footer{margin-top:2rem;font-size:.8rem;color:var(--txm);text-align:center;}\n")
          .append("    .tbtn{position:fixed;top:1rem;right:1rem;z-index:100;background:var(--bgc);border:1px solid var(--bd);border-radius:9999px;width:2.5rem;height:2.5rem;cursor:pointer;box-shadow:0 1px 3px rgba(0,0,0,.15);display:flex;align-items:center;justify-content:center;}\n")
          .append("    .ic-s{display:none;}.ic-m{display:block;}:root.dark .ic-s{display:block;}:root.dark .ic-m{display:none;}\n")
          .append("  </style>\n</head>\n<body>\n")
          .append("  <button class=\"tbtn\" onclick=\"var d=document.documentElement.classList.toggle('dark');try{localStorage.setItem('nuxt-color-mode',d?'dark':'light');}catch(e){}\" aria-label=\"Alternar tema\">\n")
          .append("    <svg class=\"ic-s\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><circle cx=\"12\" cy=\"12\" r=\"5\"/><path d=\"M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42\"/></svg>\n")
          .append("    <svg class=\"ic-m\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z\"/></svg>\n")
          .append("  </button>\n")
          .append("  <div class=\"container\">\n")
          .append("    <nav class=\"bc\"><a href=\"/\">In&iacute;cio</a> / <a href=\"/stat-proposicoes/\">Proposi&ccedil;&otilde;es Est&aacute;ticas</a></nav>\n")
          .append("    <h1>\u00cdndice de Proposi\u00e7\u00f5es Legislativas</h1>\n")
          .append("    <p class=\"stats\">").append(total).append(" proposi\u00e7\u00f5es &bull; ").append(totalCamara)
          .append(" C\u00e2mara &bull; ").append(totalSenado).append(" Senado &bull; Gerado em ").append(escHtml(generatedAt)).append("</p>\n")
          .append("    <div class=\"srch\"><input type=\"search\" id=\"q\" placeholder=\"Buscar por t\u00edtulo, ementa ou situa\u00e7\u00e3o\u2026\" oninput=\"filtrar(this.value)\" autocomplete=\"off\" /></div>\n")
          .append("    <div id=\"list\">\n");

        for (var item : items) {
            String te = escHtml(item.titulo());
            String ee = escHtml(item.ementa());
            String bc = item.casaLabel().contains("C\u00e2mara") ? "bc-c" : "bc-s";
            String bl = item.casaLabel().contains("C\u00e2mara") ? "C\u00e2mara" : "Senado";
            String dq = (item.titulo() != null ? item.titulo().toLowerCase() : "") + " "
                    + (item.ementa() != null ? item.ementa().toLowerCase() : "") + " "
                    + (item.situacao() != null ? item.situacao().toLowerCase() : "");
            sb.append("      <div class=\"item\" data-q=\"").append(escHtml(dq)).append("\">\n");
            sb.append("        <div class=\"it\"><a href=\"").append(escHtml(item.url())).append("\">").append(te).append("</a></div>\n");
            if (!ee.isBlank()) sb.append("        <div class=\"em\" title=\"").append(ee).append("\">").append(ee).append("</div>\n");
            sb.append("        <div class=\"mt\"><span class=\"badge ").append(bc).append("\">").append(bl).append("</span>");
            if (!item.situacao().isBlank()) sb.append("<span>").append(escHtml(item.situacao())).append("</span>");
            sb.append("</div>\n      </div>\n");
        }

        sb.append("    </div>\n")
          .append("    <p id=\"nr\">Nenhum resultado encontrado.</p>\n")
          .append("    <footer>Gerado em ").append(escHtml(generatedAt))
          .append(" &bull; <a href=\"https://www.translegis.com.br\">Transpar&ecirc;ncia Legislativa</a></footer>\n")
          .append("  </div>\n")
          .append("  <script>\n")
          .append("    function filtrar(q){var q2=q.trim().toLowerCase(),c=0;\n")
          .append("      document.querySelectorAll('.item').forEach(function(el){\n")
          .append("        var s=!q2||el.dataset.q.indexOf(q2)>=0;el.style.display=s?'':'none';if(s)c++;});\n")
          .append("      document.getElementById('nr').style.display=q2&&c===0?'block':'none';}\n")
          .append("  </script>\n")
          .append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
