package br.leg.congresso.etl.pagegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverCamaraDespesa;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoRelatoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.pagegen.dto.DeputadoFrenteDTO;
import br.leg.congresso.etl.pagegen.dto.DeputadoOrgaoDTO;
import br.leg.congresso.etl.pagegen.dto.DeputadoPageDTO;
import br.leg.congresso.etl.pagegen.dto.DespesaResumoDTO;
import br.leg.congresso.etl.pagegen.dto.ParlamentarIndexItemDTO;
import br.leg.congresso.etl.pagegen.dto.ProposicaoResumoDTO;
import br.leg.congresso.etl.pagegen.dto.SenadorPageDTO;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoFrenteRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoOrgaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraDespesaRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRelatoriaRepository;
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
 * {@code outputDir/stat-parlamentares/camara-{camaraId}/index.html}</li>
 * <li>Repete o processo para senadores →
 * {@code outputDir/stat-parlamentares/senado-{codigoSenador}/index.html}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParlamentaresPageGeneratorService {

    static final String PARLAMENTARES_BASE_PATH = "parlamentares";
    static final String PARLAMENTARES_STAT_PATH = "stat-parlamentares";
    static final String CAMARA_SLUG_PREFIX = "camara-";
    static final String SENADO_SLUG_PREFIX = "senado-";

    private final SilverCamaraDeputadoRepository deputadoRepository;
    private final SilverSenadoSenadorRepository senadorRepository;
    private final SilverCamaraDeputadoOrgaoRepository deputadoOrgaoRepository;
    private final SilverCamaraDeputadoFrenteRepository deputadoFrenteRepository;
    private final SilverCamaraDespesaRepository despesaRepository;
    private final SilverCamaraProposicaoAutorRepository proposicaoAutorRepository;
    private final SilverCamaraProposicaoRepository silverCamaraProposicaoRepository;
    private final SilverSenadoAutoriaRepository senadoAutoriaRepository;
    private final SilverSenadoRelatoriaRepository senadoRelatoriaRepository;
    private final ProposicaoRepository proposicaoRepository;
    private final ThymeleafRenderer renderer;
    private final ParlamentaresSitemapGenerator sitemapGenerator;
    private final ObjectMapper objectMapper;

    @Value("${etl.pagegen.output-dir:open-data/public}")
    private String outputDirConfig;

    @Value("${etl.pagegen.batch-size:500}")
    private int batchSize;

    /** Número de itens por página no índice de parlamentares. */
    private static final int INDEX_PAGE_SIZE = 100;

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter INDEX_DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat BR_CURRENCY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final int MAX_ORGAOS = 6;
    private static final int MAX_FRENTES = 6;
    private static final int MAX_PROPOSICOES = 8;
    private static final int MAX_SENADO_AUTORIAS = 8;
    private static final int MAX_SENADO_RELATORIAS = 8;

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
        generateIndex(base);
        try {
            sitemapGenerator.generate(base);
        } catch (Exception e) {
            log.warn("Falha ao gerar sitemap de parlamentares: {}", e.getMessage());
        }
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
                    writeHtml(base, buildDeputadoOutputPath(d.getCamaraId()), html);
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
                    writeHtml(base, buildSenadorOutputPath(s.getCodigoSenador()), html);
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
    List<DeputadoOrgaoDTO> orgaos = buildOrgaos(d.getCamaraId());
    List<DeputadoFrenteDTO> frentes = buildFrentes(d.getCamaraId());
    DespesaResumoDTO despesasResumo = buildDespesasResumo(d.getCamaraId());
    List<ProposicaoResumoDTO> proposicoesRecentes = buildProposicoesRecentes(d.getCamaraId());

        String localNasc = buildLocalNascimento(d.getMunicipioNascimento(), d.getUfNascimento());

    String canonicalUrl = BASE_URL + "/" + buildDeputadoOutputPath(d.getCamaraId()) + "/";
        String seoTitle = nome + " — Deputado(a) Federal"
                + (!"—".equals(partido) ? " (" + partido + "/" + uf + ")" : "");
        String seoDescription = "Perfil do(a) Deputado(a) Federal " + nome
                + (!"—".equals(partido) ? ", " + partido + "/" + uf : "")
        + ". Dados biográficos, atuação legislativa, proposições com autoria e informações de contato.";
        String email = d.getContatoEmail();
        String schemaOrgPersonJson = buildDeputadoPersonJson(
            nome,
            seoDescription,
            canonicalUrl,
            urlFoto,
            d.getSexo(),
            email,
            d.getDataNascimento(),
            localNasc,
            partido,
            d.getUrlWebsite(),
            redesSociais);
        String schemaOrgBreadcrumbJson = buildBreadcrumbJson(nome, canonicalUrl);

        var dto = DeputadoPageDTO.builder()
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
                .email(email)
            .urlFoto(urlFoto)
            .urlWebsite(d.getUrlWebsite())
            .gabineteNome(d.getDetGabineteNome())
            .gabineteEndereco(gabineteEndereco)
            .gabineteEmail(d.getDetGabineteEmail())
            .gabineteTelefone(d.getDetGabineteTelefone())
            .redesSociais(redesSociais)
            .primeiraLegislatura(d.getPrimeiraLegislatura())
            .ultimaLegislatura(d.getUltimaLegislatura())
            .orgaos(orgaos)
            .frentes(frentes)
            .despesasResumo(despesasResumo)
            .proposicoesRecentes(proposicoesRecentes)
            .canonicalUrl(canonicalUrl)
            .seoTitle(seoTitle)
            .seoDescription(seoDescription)
            .schemaOrgPersonJson(schemaOrgPersonJson)
            .schemaOrgBreadcrumbJson(schemaOrgBreadcrumbJson)
            .geradoEm(LocalDateTime.now().format(ISO_DT))
            .build();
        return dto;
    }

    private SenadorPageDTO toSenadorDTO(SilverSenadoSenador s) {
        String nome = nvl(s.getNomeParlamentar(), s.getDetNomeCompleto(), "Senador(a)");
        String partido = nvl(s.getSiglaPartidoParlamentar(), s.getPartidoParlamentar(), "—");
        String uf = nvl(s.getUfParlamentar(), "—");

        String participacaoLabel = "T".equalsIgnoreCase(s.getParticipacao()) ? "Titular"
                : "S".equalsIgnoreCase(s.getParticipacao()) ? "Suplente"
                        : s.getParticipacao();

        List<String> profissoes = parseProfissoes(s.getDetProfissoes());
    List<ProposicaoResumoDTO> autoriasRecentes = buildSenadoAutorias(s.getCodigoSenador());
    List<ProposicaoResumoDTO> relatoriasRecentes = buildSenadoRelatorias(s.getCodigoSenador());

    String canonicalUrl = BASE_URL + "/" + buildSenadorOutputPath(s.getCodigoSenador()) + "/";
        String seoTitle = nome + " — Senador(a) Federal"
                + (!"—".equals(partido) ? " (" + partido + "/" + uf + ")" : "");
        String seoDescription = "Perfil do(a) Senador(a) Federal " + nome
                + (!"—".equals(partido) ? ", " + partido + "/" + uf : "")
        + ". Dados, mandato"
        + (!autoriasRecentes.isEmpty() || !relatoriasRecentes.isEmpty()
            ? ", autoria e relatoria legislativa"
            : "")
        + " e informações de contato.";
        String schemaOrgPersonJson = buildSenadorPersonJson(
            nome,
            seoDescription,
            canonicalUrl,
            s.getDetUrlFoto(),
            s.getSexo(),
            s.getDetContatoEmail(),
            s.getDetDataNascimento(),
            s.getDetLocalNascimento(),
            partido,
            s.getDetUrlPaginaParlamentar(),
            s.getDetPagina(),
            s.getDetFacebook(),
            s.getDetTwitter());
        String schemaOrgBreadcrumbJson = buildBreadcrumbJson(nome, canonicalUrl);

        var dto = SenadorPageDTO.builder()
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
            .autoriasRecentes(autoriasRecentes)
            .relatoriasRecentes(relatoriasRecentes)
            .canonicalUrl(canonicalUrl)
            .seoTitle(seoTitle)
            .seoDescription(seoDescription)
            .schemaOrgPersonJson(schemaOrgPersonJson)
            .schemaOrgBreadcrumbJson(schemaOrgBreadcrumbJson)
            .geradoEm(LocalDateTime.now().format(ISO_DT))
            .build();
        return dto;
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

    private List<ProposicaoResumoDTO> buildSenadoAutorias(String codigoSenador) {
        if (!notBlank(codigoSenador)) {
            return List.of();
        }

        Map<String, ProposicaoResumoDTO> autorias = new LinkedHashMap<>();
        for (SilverSenadoAutoria autoria : senadoAutoriaRepository.findByCodigoParlamentarOrderByMateriaDesc(codigoSenador)) {
            SilverSenadoMateria materia = autoria.getSenadoMateria();
            ProposicaoResumoDTO resumo = toSenadoMateriaResumo(materia, null);
            if (resumo != null) {
                autorias.putIfAbsent(materia.getCodigo(), resumo);
            }
            if (autorias.size() >= MAX_SENADO_AUTORIAS) {
                break;
            }
        }
        return List.copyOf(autorias.values());
    }

    private List<ProposicaoResumoDTO> buildSenadoRelatorias(String codigoSenador) {
        Long codigoParlamentar = parseLongOrNull(codigoSenador);
        if (codigoParlamentar == null) {
            return List.of();
        }

        Map<String, ProposicaoResumoDTO> relatorias = new LinkedHashMap<>();
        for (SilverSenadoRelatoria relatoria : senadoRelatoriaRepository.findByCodigoParlamentarOrderByMateriaDesc(codigoParlamentar)) {
            SilverSenadoMateria materia = relatoria.getSenadoMateria();
            ProposicaoResumoDTO resumo = toSenadoMateriaResumo(materia, buildRelatoriaMeta(relatoria));
            if (resumo != null) {
                relatorias.putIfAbsent(materia.getCodigo(), resumo);
            }
            if (relatorias.size() >= MAX_SENADO_RELATORIAS) {
                break;
            }
        }
        return List.copyOf(relatorias.values());
    }

    private ProposicaoResumoDTO toSenadoMateriaResumo(SilverSenadoMateria materia, String meta) {
        if (materia == null || !notBlank(materia.getCodigo())) {
            return null;
        }

        Proposicao proposicao = proposicaoRepository
                .findByCasaAndIdOrigem(CasaLegislativa.SENADO, materia.getCodigo())
                .orElse(null);
        if (proposicao == null) {
            return null;
        }

        String numero = notBlank(materia.getNumero()) ? materia.getNumero().replaceFirst("^0+(?!$)", "") : "s/n";
        String ano = materia.getAno() != null ? String.valueOf(materia.getAno())
                : proposicao.getAno() != null ? String.valueOf(proposicao.getAno()) : "s/ano";
        String titulo = nvl(materia.getSigla(), proposicao.getSigla(), "Matéria") + " " + numero + "/" + ano;

        return ProposicaoResumoDTO.builder()
                .titulo(titulo)
                .ementa(nvl(proposicao.getEmenta(), materia.getEmenta()))
                .situacao(notBlank(meta) ? meta : resolveSituacaoSenado(proposicao, materia))
                .url("/stat-proposicoes/senado-" + proposicao.getIdOrigem() + "/")
                .casa("senado")
                .casaLabel("Senado Federal")
                .build();
    }

    private String buildRelatoriaMeta(SilverSenadoRelatoria relatoria) {
        List<String> partes = new ArrayList<>();
        partes.add(notBlank(relatoria.getDescricaoTipoRelator()) ? relatoria.getDescricaoTipoRelator() : "Relatoria");
        if (notBlank(relatoria.getSiglaColegiado())) {
            partes.add(relatoria.getSiglaColegiado());
        } else if (notBlank(relatoria.getNomeColegiado())) {
            partes.add(relatoria.getNomeColegiado());
        }
        return String.join(" · ", partes);
    }

    private String resolveSituacaoSenado(Proposicao proposicao, SilverSenadoMateria materia) {
        if (proposicao != null && notBlank(proposicao.getSituacao())) {
            return proposicao.getSituacao();
        }
        if (materia != null && notBlank(materia.getDetIndicadorTramitando())) {
            return "Sim".equalsIgnoreCase(materia.getDetIndicadorTramitando()) ? "Em tramitação" : "Encerrada";
        }
        return null;
    }

    private Long parseLongOrNull(String value) {
        if (!notBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<DeputadoOrgaoDTO> buildOrgaos(String camaraId) {
        return deputadoOrgaoRepository
                .findByIdDeputadoOrderByDataInicioDesc(camaraId, PageRequest.of(0, MAX_ORGAOS))
                .stream()
                .map(orgao -> DeputadoOrgaoDTO.builder()
                        .sigla(nvl(orgao.getSiglaOrgao(), orgao.getIdOrgao()))
                        .nome(nvl(orgao.getNomeOrgao(), orgao.getNomePublicacao()))
                        .cargo(nvl(orgao.getTitulo(), orgao.getCodTitulo()))
                        .periodo(formatPeriodo(orgao.getDataInicio(), orgao.getDataFim()))
                        .url(orgao.getUriOrgao())
                        .build())
                .toList();
    }

    private List<DeputadoFrenteDTO> buildFrentes(String camaraId) {
        return deputadoFrenteRepository
                .findByIdDeputadoOrderByIdLegislaturaDescTituloAsc(camaraId, PageRequest.of(0, MAX_FRENTES))
                .stream()
                .map(frente -> DeputadoFrenteDTO.builder()
                        .titulo(frente.getTitulo())
                        .legislatura(frente.getIdLegislatura())
                        .url(frente.getUri())
                        .build())
                .toList();
    }

    private DespesaResumoDTO buildDespesasResumo(String camaraId) {
        List<SilverCamaraDespesa> despesas = despesaRepository.findByCamaraDeputadoId(camaraId);
        if (despesas.isEmpty()) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        var anos = new java.util.TreeSet<String>();
        Map<String, Integer> tipos = new HashMap<>();

        for (SilverCamaraDespesa despesa : despesas) {
            total = total.add(parseMoney(despesa.getValorLiquido()));
            if (notBlank(despesa.getAno())) {
                anos.add(despesa.getAno());
            }
            if (notBlank(despesa.getTipoDespesa())) {
                tipos.merge(despesa.getTipoDespesa().trim(), 1, (atual, incremento) -> atual + incremento);
            }
        }

        String principalTipo = tipos.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .map(Map.Entry::getKey)
                .orElse(null);

        return DespesaResumoDTO.builder()
                .totalRegistros(despesas.size())
                .valorTotalFormatado(BR_CURRENCY.format(total))
                .anosCobertos(formatAnosCobertos(anos))
                .principalTipoDespesa(principalTipo)
                .totalTiposDespesa(tipos.size())
                .build();
    }

    private List<ProposicaoResumoDTO> buildProposicoesRecentes(String camaraId) {
        List<String> proposicoesIds = proposicaoAutorRepository.findByDeputadoAutorWithProposicao(camaraId).stream()
            .map(SilverCamaraProposicaoAutor::getIdProposicao)
            .filter(ParlamentaresPageGeneratorService::notBlank)
            .distinct()
            .toList();
        if (proposicoesIds.isEmpty()) {
            return List.of();
        }

        return silverCamaraProposicaoRepository.findAllByCamaraIdIn(proposicoesIds).stream()
            .sorted(Comparator
                .comparingInt(this::getAno).reversed()
                .thenComparing(Comparator.comparingInt(this::getNumero).reversed()))
                .limit(MAX_PROPOSICOES)
            .map(this::toProposicaoResumo)
                .filter(Objects::nonNull)
                .toList();
    }

    private ProposicaoResumoDTO toProposicaoResumo(SilverCamaraProposicao proposicao) {
        if (proposicao == null || !notBlank(proposicao.getCamaraId())) {
            return null;
        }
        String tituloBase = nvl(proposicao.getSiglaTipo(), "Proposição");
        String titulo = tituloBase + " "
                + (proposicao.getNumero() != null ? proposicao.getNumero() : "s/n")
                + "/"
                + (proposicao.getAno() != null ? proposicao.getAno() : "s/ano");
        return ProposicaoResumoDTO.builder()
                .titulo(titulo)
                .ementa(proposicao.getEmenta())
                .situacao(nvl(proposicao.getStatusDescricaoSituacao(), proposicao.getUltimoStatusDescricaoSituacao()))
                .url("/stat-proposicoes/camara-" + proposicao.getCamaraId() + "/")
                .casa("camara")
                .casaLabel("Câmara dos Deputados")
                .build();
    }

    private Integer getAno(SilverCamaraProposicao proposicao) {
        return proposicao != null && proposicao.getAno() != null ? proposicao.getAno() : 0;
    }

    private Integer getNumero(SilverCamaraProposicao proposicao) {
        return proposicao != null && proposicao.getNumero() != null ? proposicao.getNumero() : 0;
    }

    private String formatPeriodo(String inicio, String fim) {
        if (!notBlank(inicio) && !notBlank(fim)) {
            return null;
        }
        if (notBlank(inicio) && notBlank(fim)) {
            return inicio + " a " + fim;
        }
        return notBlank(inicio) ? "Desde " + inicio : "Até " + fim;
    }

    private String formatAnosCobertos(java.util.SortedSet<String> anos) {
        if (anos.isEmpty()) {
            return null;
        }
        if (anos.size() == 1) {
            return anos.first();
        }
        return anos.first() + " a " + anos.last();
    }

    private BigDecimal parseMoney(String valor) {
        if (!notBlank(valor)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            log.debug("Valor monetário inválido em camara_despesa: {}", valor);
            return BigDecimal.ZERO;
        }
    }

    static String buildDeputadoOutputPath(String camaraId) {
        return PARLAMENTARES_STAT_PATH + "/" + CAMARA_SLUG_PREFIX + camaraId;
    }

    static String buildSenadorOutputPath(String codigoSenador) {
        return PARLAMENTARES_STAT_PATH + "/" + SENADO_SLUG_PREFIX + codigoSenador;
    }

    @Transactional(readOnly = true)
    void generateIndex(Path base) {
        List<ParlamentarIndexItemDTO> items = new ArrayList<>();

        for (SilverCamaraDeputado deputado : deputadoRepository.findAll()) {
            if (!notBlank(deputado.getCamaraId())) {
                continue;
            }
            String nome = nvl(deputado.getNomeParlamentar(), deputado.getNomeCivil(), "Deputado(a)");
            String partido = nvl(deputado.getDetStatusSiglaPartido(), "");
            String uf = nvl(deputado.getDetStatusSiglaUf(), "");
            String slugId = CAMARA_SLUG_PREFIX + deputado.getCamaraId();
            items.add(new ParlamentarIndexItemDTO(
                    slugId,
                    nome,
                    partido,
                    uf,
                    "camara",
                    "Câmara dos Deputados",
                    nvl(deputado.getDetStatusUrlFoto(), deputado.getUrlFoto()),
                    null,
                    "/" + buildDeputadoOutputPath(deputado.getCamaraId()) + "/"));
        }

        for (SilverSenadoSenador senador : senadorRepository.findAll()) {
            if (!notBlank(senador.getCodigoSenador())) {
                continue;
            }
            String nome = nvl(senador.getNomeParlamentar(), senador.getDetNomeCompleto(), "Senador(a)");
            String partido = nvl(senador.getSiglaPartidoParlamentar(), senador.getPartidoParlamentar(), "");
            String uf = nvl(senador.getUfParlamentar(), "");
            String slugId = SENADO_SLUG_PREFIX + senador.getCodigoSenador();
            String participacao = "T".equalsIgnoreCase(senador.getParticipacao()) ? "Titular"
                    : "S".equalsIgnoreCase(senador.getParticipacao()) ? "Suplente"
                            : nvl(senador.getParticipacao(), "");
            items.add(new ParlamentarIndexItemDTO(
                    slugId,
                    nome,
                    partido,
                    uf,
                    "senado",
                    "Senado Federal",
                    senador.getDetUrlFoto(),
                    participacao,
                    "/" + buildSenadorOutputPath(senador.getCodigoSenador()) + "/"));
        }

        items.sort(Comparator.comparing(ParlamentarIndexItemDTO::nome, String.CASE_INSENSITIVE_ORDER));

        int totalItems = items.size();
        int totalPages = (int) Math.ceil((double) totalItems / INDEX_PAGE_SIZE);
        String generatedAt = LocalDateTime.now().format(INDEX_DT_FMT);

        if (totalPages == 0) {
            log.info("Nenhum parlamentar encontrado para geração do índice estático.");
            return;
        }

        log.info("Gerando índice de parlamentares: {} itens, {} páginas", totalItems, totalPages);

        for (int page = 0; page < totalPages; page++) {
            int fromIndex = page * INDEX_PAGE_SIZE;
            int toIndex = Math.min(fromIndex + INDEX_PAGE_SIZE, totalItems);
            List<ParlamentarIndexItemDTO> pageItems = items.subList(fromIndex, toIndex);
            int pageNumber = page + 1;

            try {
                Map<String, Object> pageData = new HashMap<>();
                pageData.put("currentPage", pageNumber);
                pageData.put("totalPages", totalPages);
                pageData.put("totalItems", totalItems);
                pageData.put("items", pageItems);
                writeIndexJson(base, pageNumber, objectMapper.writeValueAsString(pageData));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao serializar JSON do índice de parlamentares na página " + pageNumber,
                        e);
            }
        }

        long totalDeputados = items.stream().filter(item -> "camara".equals(item.casa())).count();
        long totalSenadores = items.stream().filter(item -> "senado".equals(item.casa())).count();

        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("totalPages", totalPages);
            meta.put("totalItems", totalItems);
            meta.put("totalDeputados", totalDeputados);
            meta.put("totalSenadores", totalSenadores);
            meta.put("generatedAt", generatedAt);
            writeIndexMeta(base, objectMapper.writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar meta.json do índice de parlamentares", e);
        }

        // Gera índice estático HTML
        try {
            generateStaticIndex(base, items, generatedAt);
        } catch (Exception e) {
            log.warn("Falha ao gerar índice estático HTML de parlamentares: {}", e.getMessage());
        }
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

    private String buildDeputadoPersonJson(
            String nomeParlamentar,
            String seoDescription,
            String canonicalUrl,
            String urlFoto,
            String sexo,
            String email,
            String dataNascimento,
            String localNascimento,
            String partido,
            String urlWebsite,
            List<String> redesSociais) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("@context", "https://schema.org");
        json.put("@type", "Person");
        json.put("name", nomeParlamentar);
        putIfNotBlank(json, "description", seoDescription);
        putIfNotBlank(json, "url", canonicalUrl);
        putIfNotBlank(json, "image", urlFoto);
        putIfNotBlank(json, "gender", sexo);
        putIfNotBlank(json, "email", email);
        putIfNotBlank(json, "jobTitle", "Deputado Federal");
        putIfNotBlank(json, "birthDate", dataNascimento);
        putIfNotBlank(json, "birthPlace", localNascimento);
        putIfNotBlank(json, "mainEntityOfPage", canonicalUrl);
        json.put("affiliation", organization("Câmara dos Deputados"));
        if (notBlank(partido) && !"—".equals(partido)) {
            json.put("memberOf", organization(partido));
        }

        List<String> sameAs = new ArrayList<>();
        if (redesSociais != null) {
            for (String rede : redesSociais) {
                addIfNotBlank(sameAs, rede);
            }
        }
        addIfNotBlank(sameAs, urlWebsite);
        if (!sameAs.isEmpty()) {
            json.put("sameAs", sameAs);
        }

        return writeJson(json, "person do deputado");
    }

    private String buildSenadorPersonJson(
            String nomeParlamentar,
            String seoDescription,
            String canonicalUrl,
            String urlFoto,
            String sexo,
            String email,
            String dataNascimento,
            String localNascimento,
            String partido,
            String urlPaginaParlamentar,
            String paginaPessoal,
            String facebook,
            String twitter) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("@context", "https://schema.org");
        json.put("@type", "Person");
        json.put("name", nomeParlamentar);
        putIfNotBlank(json, "description", seoDescription);
        putIfNotBlank(json, "url", canonicalUrl);
        putIfNotBlank(json, "image", urlFoto);
        putIfNotBlank(json, "gender", sexo);
        putIfNotBlank(json, "email", email);
        putIfNotBlank(json, "jobTitle", "Senador Federal");
        putIfNotBlank(json, "birthDate", dataNascimento);
        putIfNotBlank(json, "birthPlace", localNascimento);
        putIfNotBlank(json, "mainEntityOfPage", canonicalUrl);
        json.put("affiliation", organization("Senado Federal"));
        if (notBlank(partido) && !"—".equals(partido)) {
            json.put("memberOf", organization(partido));
        }

        List<String> sameAs = new ArrayList<>();
        addIfNotBlank(sameAs, urlPaginaParlamentar);
        addIfNotBlank(sameAs, paginaPessoal);
        addIfNotBlank(sameAs, facebook);
        addIfNotBlank(sameAs, twitter);
        if (!sameAs.isEmpty()) {
            json.put("sameAs", sameAs);
        }

        return writeJson(json, "person do senador");
    }

    private String buildBreadcrumbJson(String nomeParlamentar, String canonicalUrl) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(breadcrumbItem(1, "Início", BASE_URL + "/"));
        items.add(breadcrumbItem(2, "Parlamentares", BASE_URL + "/stat-parlamentares/"));
        items.add(breadcrumbItem(3, nomeParlamentar, canonicalUrl));

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("@context", "https://schema.org");
        json.put("@type", "BreadcrumbList");
        json.put("itemListElement", items);
        return writeJson(json, "breadcrumb de parlamentar");
    }

    private Map<String, Object> breadcrumbItem(int position, String name, String itemUrl) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("@type", "ListItem");
        item.put("position", position);
        item.put("name", name);
        item.put("item", itemUrl);
        return item;
    }

    private Map<String, Object> organization(String name) {
        Map<String, Object> org = new LinkedHashMap<>();
        org.put("@type", "Organization");
        org.put("name", name);
        return org;
    }

    private void putIfNotBlank(Map<String, Object> json, String key, String value) {
        if (notBlank(value)) {
            json.put(key, value);
        }
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (notBlank(value) && !values.contains(value)) {
            values.add(value);
        }
    }

    private String writeJson(Map<String, Object> json, String label) {
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar JSON-LD de " + label, e);
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

    void writeIndexJson(Path base, int pageNumber, String json) {
        Path dir = base.resolve(PARLAMENTARES_BASE_PATH).resolve("indice").resolve("data");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("page-" + pageNumber + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar JSON do índice de parlamentares em " + dir, e);
        }
    }

    void writeIndexMeta(Path base, String json) {
        Path dir = base.resolve(PARLAMENTARES_BASE_PATH).resolve("indice").resolve("data");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("meta.json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar meta.json do índice de parlamentares em " + dir, e);
        }
    }

    // ── Índice estático HTML ──────────────────────────────────────────────────

    void generateStaticIndex(Path base, List<ParlamentarIndexItemDTO> items, String generatedAt) {
        if (items.isEmpty()) {
            log.info("Índice estático de parlamentares: nenhum item, ignorando.");
            return;
        }
        String html = buildParlamentaresIndexHtml(items, generatedAt);
        Path indexFile = base.resolve(PARLAMENTARES_STAT_PATH).resolve("index.html");
        try {
            Files.createDirectories(indexFile.getParent());
            Files.writeString(indexFile, html, StandardCharsets.UTF_8);
            log.info("Índice estático de parlamentares gravado: {} ({} itens)", indexFile, items.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar índice estático de parlamentares", e);
        }
    }

    private String buildParlamentaresIndexHtml(List<ParlamentarIndexItemDTO> items, String generatedAt) {
        int total = items.size();
        long totalCamara = items.stream().filter(i -> "camara".equals(i.casa())).count();
        long totalSenado = total - totalCamara;
        var sb = new StringBuilder(total * 320 + 12000);
        sb.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\" />\n")
          .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n")
          .append("  <title>Índice de Parlamentares — Transparência Legislativa</title>\n")
          .append("  <meta name=\"description\" content=\"Lista completa de ").append(total)
          .append(" parlamentares do Congresso Nacional.\" />\n")
                    .append("  <link rel=\"canonical\" href=\"https://www.translegis.com.br/stat-parlamentares/\" />\n");
                StaticPageGlobalHead.appendCommonHeadTags(sb);
                sb
          .append("  <style>\n")
          .append("    :root{--bg:#f9fafb;--bgc:#fff;--tx:#111827;--txm:#6b7280;--bd:#e5e7eb;--hd:#1a4480;--lk:#3b82f6;--sp:#f3f4f6;}\n")
          .append("    :root.dark{--bg:#111827;--bgc:#1f2937;--tx:#f1f5f9;--txm:#9ca3af;--bd:#374151;--hd:#60a5fa;--lk:#93c5fd;--sp:#374151;}\n")
          .append("    *{box-sizing:border-box;margin:0;padding:0;}\n")
          .append("    body{font-family:system-ui,-apple-system,sans-serif;background:var(--bg);color:var(--tx);line-height:1.5;}\n")
          .append("    .container{max-width:1100px;margin:0 auto;padding:2rem 1rem;}\n")
          .append("    h1{color:var(--hd);font-size:1.5rem;margin:.25rem 0 .5rem;}\n")
          .append("    a{color:var(--lk);text-decoration:none;}a:hover{text-decoration:underline;}\n")
          .append("    .bc{font-size:.875rem;color:var(--txm);margin-bottom:1.25rem;}\n")
          .append("    .bc a{color:var(--lk);}\n")
          .append("    .stats{font-size:.875rem;color:var(--txm);margin-bottom:1.5rem;}\n")
          .append("    .srch{margin:1rem 0 1.5rem;}\n")
          .append("    .srch input{width:100%;padding:.625rem 1rem;border:1px solid var(--bd);border-radius:.5rem;background:var(--bgc);color:var(--tx);font-size:1rem;}\n")
          .append("    .srch input:focus{outline:2px solid var(--lk);border-color:transparent;}\n")
          .append("    .srch input::placeholder{color:var(--txm);}\n")
          .append("    #list{border:1px solid var(--bd);border-radius:.5rem;overflow:hidden;}\n")
          .append("    .item{display:flex;align-items:center;gap:.75rem;padding:.625rem .875rem;border-bottom:1px solid var(--bd);background:var(--bgc);}\n")
          .append("    .item:last-child{border-bottom:none;}\n")
          .append("    .ph{width:2.5rem;height:3rem;border-radius:.25rem;object-fit:cover;flex-shrink:0;background:var(--sp);}\n")
          .append("    .phx{width:2.5rem;height:3rem;border-radius:.25rem;flex-shrink:0;background:var(--sp);display:flex;align-items:center;justify-content:center;font-size:1.25rem;color:var(--txm);}\n")
          .append("    .nm{font-weight:600;color:var(--tx);flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}\n")
          .append("    .mt{font-size:.8rem;color:var(--txm);white-space:nowrap;}\n")
          .append("    .badge{display:inline-block;padding:.15rem .5rem;border-radius:9999px;font-size:.7rem;font-weight:600;}\n")
          .append("    .bc-c{background:#dcfce7;color:#166534;}.bc-s{background:#dbeafe;color:#1e40af;}\n")
          .append("    :root.dark .bc-c{background:#14532d;color:#86efac;}:root.dark .bc-s{background:#1e3a5f;color:#93c5fd;}\n")
          .append("    #nr{display:none;padding:2rem;text-align:center;color:var(--txm);}\n")
          .append("    footer{margin-top:2rem;font-size:.8rem;color:var(--txm);text-align:center;padding:.5rem 0;}\n")
          .append("    .tbtn{position:fixed;top:1rem;right:1rem;z-index:100;background:var(--bgc);border:1px solid var(--bd);border-radius:9999px;width:2.5rem;height:2.5rem;cursor:pointer;box-shadow:0 1px 3px rgba(0,0,0,.15);display:flex;align-items:center;justify-content:center;}\n")
          .append("    .ic-s{display:none;}.ic-m{display:block;}:root.dark .ic-s{display:block;}:root.dark .ic-m{display:none;}\n")
          .append("    @media(max-width:640px){.mt{display:none;}}\n")
          .append("  </style>\n</head>\n<body>\n")
          .append("  <button class=\"tbtn\" onclick=\"var d=document.documentElement.classList.toggle('dark');try{localStorage.setItem('nuxt-color-mode',d?'dark':'light');}catch(e){}\" aria-label=\"Alternar tema\">\n")
          .append("    <svg class=\"ic-s\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><circle cx=\"12\" cy=\"12\" r=\"5\"/><path d=\"M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42\"/></svg>\n")
          .append("    <svg class=\"ic-m\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><path d=\"M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z\"/></svg>\n")
          .append("  </button>\n")
          .append("  <div class=\"container\">\n")
          .append("    <nav class=\"bc\"><a href=\"/\">In&iacute;cio</a> / <a href=\"/stat-parlamentares/\">Parlamentares Estat&aacute;ticos</a></nav>\n")
          .append("    <h1>Índice de Parlamentares</h1>\n")
          .append("    <p class=\"stats\">").append(total).append(" parlamentares &bull; ").append(totalCamara)
          .append(" deputados &bull; ").append(totalSenado).append(" senadores &bull; Gerado em ").append(escHtml(generatedAt)).append("</p>\n")
          .append("    <div class=\"srch\"><input type=\"search\" id=\"q\" placeholder=\"Buscar por nome, partido ou UF\u2026\" oninput=\"filtrar(this.value)\" autocomplete=\"off\" /></div>\n")
          .append("    <div id=\"list\">\n");

        for (var item : items) {
            String ne = escHtml(item.nome());
            String bc = "camara".equals(item.casa()) ? "bc-c" : "bc-s";
            String bl = "camara".equals(item.casa()) ? "C\u00e2mara" : "Senado";
            String mt = (item.partido() != null && !item.partido().isBlank() ? escHtml(item.partido()) : "")
                    + (item.uf() != null && !item.uf().isBlank() ? " &bull; " + escHtml(item.uf()) : "");
            String dq = (item.nome() != null ? item.nome().toLowerCase() : "") + " "
                    + (item.partido() != null ? item.partido().toLowerCase() : "") + " "
                    + (item.uf() != null ? item.uf().toLowerCase() : "");
            sb.append("      <div class=\"item\" data-q=\"").append(escHtml(dq)).append("\">\n");
            if (item.urlFoto() != null && !item.urlFoto().isBlank()) {
                sb.append("        <img class=\"ph\" src=\"").append(escHtml(item.urlFoto()))
                  .append("\" alt=\"\" loading=\"lazy\" onerror=\"this.style.display='none'\" />\n");
            } else {
                sb.append("        <div class=\"phx\">\uD83D\uDC64</div>\n");
            }
            sb.append("        <a class=\"nm\" href=\"").append(escHtml(item.url())).append("\" title=\"").append(ne).append("\">").append(ne).append("</a>\n");
            sb.append("        <span class=\"mt\">").append(mt).append("</span>\n");
            sb.append("        <span class=\"badge ").append(bc).append("\">").append(bl).append("</span>\n");
            sb.append("      </div>\n");
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
