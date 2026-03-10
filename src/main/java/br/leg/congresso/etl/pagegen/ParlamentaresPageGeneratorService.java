package br.leg.congresso.etl.pagegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.pagegen.dto.DeputadoPageDTO;
import br.leg.congresso.etl.pagegen.dto.SenadorPageDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de geração de páginas estáticas HTML para parlamentares.
 *
 * <p>
 * Fluxo:
 * <ol>
 * <li>Pagina os deputados Silver em lotes de {@code batchSize}</li>
 * <li>Monta {@link DeputadoPageDTO} e renderiza via
 * {@link ThymeleafRenderer}</li>
 * <li>Grava em
 * {@code outputDir/parlamentares/deputados/{camaraId}/index.html}</li>
 * <li>Repete o processo para senadores →
 * {@code senadores/{codigoSenador}/index.html}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParlamentaresPageGeneratorService {

    private final SilverCamaraDeputadoRepository deputadoRepository;
    private final SilverSenadoSenadorRepository senadorRepository;
    private final ThymeleafRenderer renderer;
    private final ObjectMapper objectMapper;

    @Value("${etl.pagegen.output-dir:open-data/public}")
    private String outputDirConfig;

    @Value("${etl.pagegen.batch-size:500}")
    private int batchSize;

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Gera todas as páginas de deputados e senadores.
     *
     * @return array com [totalDeputados, totalSenadores]
     */
    @Transactional(readOnly = true)
    public int[] generateAll() {
        Path base = resolveOutputDir();
        int deputados = generateDeputados(base);
        int senadores = generateSenadores(base);
        log.info("Geração de páginas de parlamentares concluída: {} deputados, {} senadores",
                deputados, senadores);
        return new int[] { deputados, senadores };
    }

    // ── Deputados ────────────────────────────────────────────────────────────

    private int generateDeputados(Path base) {
        long total = deputadoRepository.count();
        if (total == 0) {
            log.info("Nenhum deputado encontrado na Silver — pulando geração de páginas de deputados.");
            return 0;
        }
        int pages = (int) Math.ceil((double) total / batchSize);
        var counter = new AtomicInteger(0);
        var errors = new AtomicInteger(0);

        log.info("Gerando páginas de deputados: {} registros, {} lotes", total, pages);
        for (int page = 0; page < pages; page++) {
            var pageable = PageRequest.of(page, batchSize);
            var batch = deputadoRepository.findAll(pageable).getContent();
            for (SilverCamaraDeputado d : batch) {
                if (d.getCamaraId() == null || d.getCamaraId().isBlank()) {
                    errors.incrementAndGet();
                    continue;
                }
                try {
                    var dto = toDeputadoDTO(d);
                    var html = renderer.render("parlamentares/deputado", Map.of("page", dto));
                    writeHtml(base, "parlamentares/deputados/" + d.getCamaraId(), html);
                    counter.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Falha ao gerar página para deputado camaraId={}: {}", d.getCamaraId(), e.getMessage());
                    errors.incrementAndGet();
                }
            }
            log.info("Lote deputados {}/{}: geradas={}, erros={}", page + 1, pages, counter.get(), errors.get());
        }
        log.info("Deputados: {} páginas geradas, {} erros", counter.get(), errors.get());
        return counter.get();
    }

    // ── Senadores ────────────────────────────────────────────────────────────

    private int generateSenadores(Path base) {
        long total = senadorRepository.count();
        if (total == 0) {
            log.info("Nenhum senador encontrado na Silver — pulando geração de páginas de senadores.");
            return 0;
        }
        int pages = (int) Math.ceil((double) total / batchSize);
        var counter = new AtomicInteger(0);
        var errors = new AtomicInteger(0);

        log.info("Gerando páginas de senadores: {} registros, {} lotes", total, pages);
        for (int page = 0; page < pages; page++) {
            var pageable = PageRequest.of(page, batchSize);
            var batch = senadorRepository.findAll(pageable).getContent();
            for (SilverSenadoSenador s : batch) {
                if (s.getCodigoSenador() == null || s.getCodigoSenador().isBlank()) {
                    errors.incrementAndGet();
                    continue;
                }
                try {
                    var dto = toSenadorDTO(s);
                    var html = renderer.render("parlamentares/senador", Map.of("page", dto));
                    writeHtml(base, "parlamentares/senadores/" + s.getCodigoSenador(), html);
                    counter.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Falha ao gerar página para senador codigo={}: {}", s.getCodigoSenador(), e.getMessage());
                    errors.incrementAndGet();
                }
            }
            log.info("Lote senadores {}/{}: geradas={}, erros={}", page + 1, pages, counter.get(), errors.get());
        }
        log.info("Senadores: {} páginas geradas, {} erros", counter.get(), errors.get());
        return counter.get();
    }

    // ── Montagem de DTOs ─────────────────────────────────────────────────────

    private DeputadoPageDTO toDeputadoDTO(SilverCamaraDeputado d) {
        String nome = nvl(d.getNomeParlamentar(), d.getNomeCivil(), "Deputado(a)");
        String partido = nvl(d.getDetStatusSiglaPartido(), "—");
        String uf = nvl(d.getDetStatusSiglaUf(), "—");
        String urlFoto = nvl(d.getDetStatusUrlFoto(), d.getUrlFoto());

        String gabineteEndereco = buildGabineteEndereco(d);

        List<String> redesSociais = parseRedeSocial(d.getDetRedeSocial());

        String localNasc = buildLocalNascimento(d.getMunicipioNascimento(), d.getUfNascimento());

        String canonicalUrl = BASE_URL + "/parlamentares/deputados/" + d.getCamaraId();
        String seoTitle = nome + " — Deputado(a) Federal"
                + (!"—".equals(partido) ? " (" + partido + "/" + uf + ")" : "");
        String seoDescription = "Perfil do(a) Deputado(a) Federal " + nome
                + (!"—".equals(partido) ? ", " + partido + "/" + uf : "")
                + ". Dados, mandato e informações de contato.";

        return DeputadoPageDTO.builder()
                .camaraId(d.getCamaraId())
                .nomeParlamentar(nome)
                .nomeCivil(d.getNomeCivil())
                .nomeEleitoral(nvl(d.getDetStatusNomeEleitoral(), d.getNomeEleitoral()))
                .sexo(d.getSexo())
                .partido(partido)
                .uf(uf)
                .situacao(d.getDetStatusSituacao())
                .condicaoEleitoral(d.getDetStatusCondicaoEleitoral())
                .descricaoSituacao(d.getDetStatusDescricao())
                .dataNascimento(d.getDataNascimento())
                .localNascimento(localNasc)
                .escolaridade(d.getEscolaridade())
                .email(nvl(d.getDetStatusEmail(), d.getDetGabineteEmail()))
                .urlFoto(urlFoto)
                .urlWebsite(d.getUrlWebsite())
                .gabineteNome(d.getDetGabineteNome())
                .gabineteEndereco(gabineteEndereco)
                .gabineteEmail(d.getDetGabineteEmail())
                .gabineteTelefone(d.getDetGabineteTelefone())
                .redesSociais(redesSociais)
                .primeiraLegislatura(d.getPrimeiraLegislatura())
                .ultimaLegislatura(d.getUltimaLegislatura())
                .canonicalUrl(canonicalUrl)
                .seoTitle(seoTitle)
                .seoDescription(seoDescription)
                .geradoEm(LocalDateTime.now().format(ISO_DT))
                .build();
    }

    private SenadorPageDTO toSenadorDTO(SilverSenadoSenador s) {
        String nome = nvl(s.getNomeParlamentar(), s.getDetNomeCompleto(), "Senador(a)");
        String partido = nvl(s.getSiglaPartidoParlamentar(), s.getPartidoParlamentar(), "—");
        String uf = nvl(s.getUfParlamentar(), "—");

        String participacaoLabel = "T".equalsIgnoreCase(s.getParticipacao()) ? "Titular"
                : "S".equalsIgnoreCase(s.getParticipacao()) ? "Suplente"
                        : s.getParticipacao();

        List<String> profissoes = parseProfissoes(s.getDetProfissoes());

        String canonicalUrl = BASE_URL + "/parlamentares/senadores/" + s.getCodigoSenador();
        String seoTitle = nome + " — Senador(a) Federal"
                + (!"—".equals(partido) ? " (" + partido + "/" + uf + ")" : "");
        String seoDescription = "Perfil do(a) Senador(a) Federal " + nome
                + (!"—".equals(partido) ? ", " + partido + "/" + uf : "")
                + ". Dados, mandato e informações de contato.";

        return SenadorPageDTO.builder()
                .codigoSenador(s.getCodigoSenador())
                .nomeParlamentar(nome)
                .nomeCompleto(s.getDetNomeCompleto())
                .sexo(s.getSexo())
                .partido(partido)
                .uf(uf)
                .participacao(participacaoLabel)
                .legislatura(s.getCodigoLegislatura())
                .dataNascimento(s.getDetDataNascimento())
                .localNascimento(s.getDetLocalNascimento())
                .escolaridade(s.getDetEscolaridade())
                .estadoCivil(s.getDetEstadoCivil())
                .profissoes(profissoes)
                .email(s.getDetContatoEmail())
                .urlFoto(s.getDetUrlFoto())
                .urlPaginaParlamentar(s.getDetUrlPaginaParlamentar())
                .paginaPessoal(s.getDetPagina())
                .facebook(s.getDetFacebook())
                .twitter(s.getDetTwitter())
                .canonicalUrl(canonicalUrl)
                .seoTitle(seoTitle)
                .seoDescription(seoDescription)
                .geradoEm(LocalDateTime.now().format(ISO_DT))
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String nvl(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank())
                return v;
        }
        return null;
    }

    private static String buildLocalNascimento(String municipio, String uf) {
        if (municipio == null && uf == null)
            return null;
        if (municipio == null)
            return uf;
        if (uf == null)
            return municipio;
        return municipio + " - " + uf;
    }

    private static String buildGabineteEndereco(SilverCamaraDeputado d) {
        List<String> partes = new ArrayList<>();
        if (notBlank(d.getDetGabinetePredio()))
            partes.add("Prédio " + d.getDetGabinetePredio());
        if (notBlank(d.getDetGabineteAndar()))
            partes.add("Andar " + d.getDetGabineteAndar());
        if (notBlank(d.getDetGabineteSala()))
            partes.add("Sala " + d.getDetGabineteSala());
        return partes.isEmpty() ? null : String.join(", ", partes);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private List<String> parseRedeSocial(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            // JSON array de strings (URLs) conforme retornado pela API Câmara
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.debug("Falha ao parsear det_rede_social: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> parseProfissoes(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            // JSON array de strings conforme armazenado pelo
            // SenadoSenadorSubrecursosApiExtractor
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.debug("Falha ao parsear det_profissoes: {}", e.getMessage());
            return List.of();
        }
    }

    // ── I/O ─────────────────────────────────────────────────────────────────

    private Path resolveOutputDir() {
        Path p = Path.of(outputDirConfig);
        if (!p.isAbsolute()) {
            p = Path.of(System.getProperty("user.dir")).resolve(p);
        }
        return p;
    }

    private void writeHtml(Path base, String slug, String html) {
        Path dir = base.resolve(slug);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("index.html"), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar HTML em " + dir, e);
        }
    }
}
