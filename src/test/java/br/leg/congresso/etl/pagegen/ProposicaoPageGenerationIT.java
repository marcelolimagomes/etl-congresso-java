package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;
import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoTemaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;

/**
 * Teste de integração da geração de páginas HTML estáticas.
 *
 * <p>
 * Requer PostgreSQL disponível em localhost:5433 (docker-compose up).
 * Para personalizar: {@code -Dtest.datasource.url=jdbc:postgresql://...}
 *
 * <p>
 * Verifica o pipeline completo: banco de dados (Gold + Silver) → assembler →
 * Thymeleaf → arquivo HTML.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("ProposicaoPageGenerationIT — pipeline completo Gold+Silver → HTML")
class ProposicaoPageGenerationIT {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> System.getProperty("test.datasource.url",
                        "jdbc:postgresql://localhost:5433/etl_congresso"));
        registry.add("spring.datasource.username",
                () -> System.getProperty("test.datasource.username", "etl_user"));
        registry.add("spring.datasource.password",
                () -> System.getProperty("test.datasource.password", "etl_pass"));
    }

    @Autowired
    private PageGeneratorService pageGeneratorService;

    @Autowired
    private ProposicaoRepository proposicaoRepository;

    @Autowired
    private SilverCamaraProposicaoRepository silverCamaraProposicaoRepository;

    @Autowired
    private SilverCamaraProposicaoAutorRepository silverCamaraAutorRepository;

    @Autowired
    private SilverCamaraProposicaoTemaRepository silverCamaraProposicaoTemaRepository;

    @Autowired
    private SilverSenadoMateriaRepository silverSenadoMateriaRepository;

    @Autowired
    private SilverSenadoAutoriaRepository silverSenadoAutoriaRepository;

        @Autowired
        private SilverSenadoSenadorRepository silverSenadoSenadorRepository;

    private Path tempOutputDir;

    @BeforeEach
    void setUp() throws IOException {
        tempOutputDir = Files.createTempDirectory("pagegen-it");
        ReflectionTestUtils.setField(pageGeneratorService, "outputDirConfig", tempOutputDir.toString());
        ReflectionTestUtils.setField(pageGeneratorService, "batchSize", 100);
        limpaBanco();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempOutputDir);
        limpaBanco();
    }

    /**
     * Apaga todos os dados de teste na ordem correta para respeitar as FKs:
     * Gold primeiro (referencia Silver), depois filhos Silver, depois pais Silver.
     */
    private void limpaBanco() {
        // Gold primeiro (FK para Silver)
        proposicaoRepository.deleteAll();
        // Filhos Silver Câmara
        silverCamaraAutorRepository.deleteAll();
        silverCamaraProposicaoTemaRepository.deleteAll();
        // Pai Silver Câmara
        silverCamaraProposicaoRepository.deleteAll();
        // Filhos Silver Senado
        silverSenadoAutoriaRepository.deleteAll();
        silverSenadoSenadorRepository.deleteAll();
        // Pai Silver Senado
        silverSenadoMateriaRepository.deleteAll();
    }

    // ── Testes básicos Câmara / Senado ────────────────────────────────────────

    @Test
    @DisplayName("gera HTML para proposição Câmara com SEO title, canonical URL e schema.org")
    void geraHtmlParaProposicaoCamara() throws IOException {
        var proposicao = Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PL")
                .numero(9901)
                .ano(2024)
                .ementa("Proposta de integração para testes automatizados.")
                .situacao("Em tramitação")
                .dataApresentacao(LocalDate.of(2024, 1, 15))
                .idOrigem("990001")
                .build();
        proposicaoRepository.save(proposicao);

        int count = pageGeneratorService.generateAll();
        assertThat(count).isGreaterThanOrEqualTo(1);

        Path htmlFile = tempOutputDir.resolve("stat-proposicoes")
                .resolve("camara-990001")
                .resolve("index.html");
        assertThat(htmlFile).exists();

        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        // SEO title no formato: {sigla} {numero}/{ano} — {casaLabel} | {siteName}
        assertThat(html).contains("PL 9901/2024 — Câmara dos Deputados | Transparência Legislativa");
        // Canonical URL
        assertThat(html).contains("translegis.com.br/stat-proposicoes/camara-990001/");
        // Casa label no corpo da página
        assertThat(html).contains("Câmara dos Deputados");
        // Ementa
        assertThat(html).contains("Proposta de integração para testes automatizados.");
        // Schema.org JSON-LD Legislation
        assertThat(html).contains("\"@type\":\"Legislation\"");
        // Schema.org BreadcrumbList
        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        // Open Graph type (hardcoded no template)
        assertThat(html).contains("og:type").contains("article");
                // Assets globais de SEO/monetização/analytics
                assertThat(html).contains("name=\"google-adsense-account\"");
                assertThat(html).contains("pagead2.googlesyndication.com/pagead/js/adsbygoogle.js");
                assertThat(html).contains("www.googletagmanager.com/gtag/js?id=G-RR9S2KQ44D");
                assertThat(html).contains("gtag('config', 'G-RR9S2KQ44D')");
                assertThat(html).contains("name=\"robots\"");
    }

    @Test
    @DisplayName("gera HTML para proposição Senado com SEO title correto")
    void geraHtmlParaProposicaoSenado() throws IOException {
        var proposicao = Proposicao.builder()
                .casa(CasaLegislativa.SENADO)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PLS")
                .numero(9902)
                .ano(2023)
                .ementa("Matéria do Senado para teste de integração.")
                .situacao("Em tramitação")
                .dataApresentacao(LocalDate.of(2023, 6, 20))
                .idOrigem("990002")
                .build();
        proposicaoRepository.save(proposicao);

        pageGeneratorService.generateAll();

        Path htmlFile = tempOutputDir.resolve("stat-proposicoes")
                .resolve("senado-990002")
                .resolve("index.html");
        assertThat(htmlFile).exists();

        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        // SEO title com "Senado Federal"
        assertThat(html).contains("PLS 9902/2023 — Senado Federal | Transparência Legislativa");
        assertThat(html).contains("Senado Federal");
    }

    // ── Consistência de dados Gold → HTML ─────────────────────────────────────

    @Test
    @DisplayName("HTML reflete exatamente os dados do banco — ementa, data, situação e URL íntegro teor")
    void htmlRefleteDadosDoBanco() throws IOException {
        var proposicao = Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.EMENDA_CONSTITUCIONAL)
                .sigla("PEC")
                .numero(42)
                .ano(2023)
                .ementa("Reforma do sistema tributário para testes de dados.")
                .situacao("Em tramitacao IT")
                .dataApresentacao(LocalDate.of(2023, 5, 10))
                .urlInteiroTeor("https://camara.leg.br/pl/pec42")
                .idOrigem("990003")
                .build();
        proposicaoRepository.save(proposicao);

        pageGeneratorService.generateAll();

        Path htmlFile = tempOutputDir.resolve("stat-proposicoes")
                .resolve("camara-990003")
                .resolve("index.html");
        assertThat(htmlFile).exists();

        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        // SEO title
        assertThat(html).contains("PEC 42/2023 — Câmara dos Deputados | Transparência Legislativa");
        // Canonical URL com slug correto
        assertThat(html).contains("translegis.com.br/stat-proposicoes/camara-990003/");
        // Texto da ementa exato
        assertThat(html).contains("Reforma do sistema tributário para testes de dados.");
        // Situação
        assertThat(html).contains("Em tramitacao IT");
        // Data em formato brasileiro (dd/MM/yyyy)
        assertThat(html).contains("10/05/2023");
        // URL íntegro teor
        assertThat(html).contains("https://camara.leg.br/pl/pec42");
    }

    // ── Integração Silver Câmara ──────────────────────────────────────────────

    @Test
    @DisplayName("HTML exibe autores, temas e ementa detalhada da camada Silver Câmara")
    void geraHtmlComDadosSilverCamara() throws IOException {
        // Silver Câmara — proposição com ementa detalhada
        SilverCamaraProposicao silverCamara = silverCamaraProposicaoRepository.save(
                SilverCamaraProposicao.builder()
                        .ingeridoEm(LocalDateTime.now())
                        .ementaDetalhada("Ementa detalhada completa para teste IT.")
                        .build());

        // Silver Câmara — autor parlamentar com link de perfil
        silverCamaraAutorRepository.save(
                SilverCamaraProposicaoAutor.builder()
                        .ingeridoEm(LocalDateTime.now())
                        .camaraProposicao(silverCamara)
                        .nomeAutor("João Silva IT")
                        .idDeputadoAutor("204560")
                        .siglaPartidoAutor("PT")
                        .siglaUfAutor("SP")
                        .proponente(1)
                        .build());

        // Silver Câmara — tema
        silverCamaraProposicaoTemaRepository.save(
                SilverCamaraProposicaoTema.builder()
                        .ingeridoEm(LocalDateTime.now())
                        .camaraProposicao(silverCamara)
                        .codTema(11)
                        .tema("Administração Pública IT")
                        .build());

        // Gold vinculado ao Silver
        proposicaoRepository.save(Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PL")
                .numero(9905)
                .ano(2024)
                .ementa("Proposição com Silver Câmara.")
                .situacao("Em tramitação")
                .dataApresentacao(LocalDate.of(2024, 2, 1))
                .idOrigem("990005")
                .silverCamaraId(silverCamara.getId())
                .build());

        pageGeneratorService.generateAll();

        String html = Files.readString(
                tempOutputDir.resolve("stat-proposicoes").resolve("camara-990005").resolve("index.html"),
                StandardCharsets.UTF_8);

        // Nome do autor da camada Silver
        assertThat(html).contains("João Silva IT");
        // Link para perfil do parlamentar na Câmara
        assertThat(html).contains("/stat-parlamentares/camara-204560/");
        // Partido/UF do autor
        assertThat(html).contains("PT/SP");
        // Tema da camada Silver
        assertThat(html).contains("Administração Pública IT");
        // Ementa detalhada da camada Silver
        assertThat(html).contains("Ementa detalhada completa para teste IT.");
    }

    // ── Integração Silver Senado ──────────────────────────────────────────────

    @Test
    @DisplayName("HTML exibe autores da camada Silver Senado")
    void geraHtmlComDadosSilverSenado() throws IOException {
        // Silver Senado — matéria
        SilverSenadoMateria silverSenado = silverSenadoMateriaRepository.save(
                SilverSenadoMateria.builder()
                        .ingeridoEm(LocalDateTime.now())
                        .codigo("IT-SIT-001")
                        .build());

        // Silver Senado — autoria parlamentar com link de perfil
        silverSenadoAutoriaRepository.save(
                SilverSenadoAutoria.builder()
                        .ingeridoEm(LocalDateTime.now())
                        .senadoMateria(silverSenado)
                        .nomeAutor("Maria Fontes IT")
                        .codigoTipoAutor("Parlamentar")
                        .codigoParlamentar("9999")
                        .siglaPartido("MDB")
                        .ufParlamentar("RJ")
                        .build());

        silverSenadoSenadorRepository.save(
                SilverSenadoSenador.builder()
                        .codigoSenador("9999")
                        .nomeParlamentar("Maria Fontes IT")
                        .ufParlamentar("RJ")
                        .siglaPartidoParlamentar("MDB")
                        .build());

        // Gold vinculado ao Silver Senado
        proposicaoRepository.save(Proposicao.builder()
                .casa(CasaLegislativa.SENADO)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PLS")
                .numero(9906)
                .ano(2023)
                .ementa("Proposição com Silver Senado.")
                .situacao("Em tramitação")
                .dataApresentacao(LocalDate.of(2023, 4, 15))
                .idOrigem("990006")
                .silverSenadoId(silverSenado.getId())
                .build());

        pageGeneratorService.generateAll();

        String html = Files.readString(
                tempOutputDir.resolve("stat-proposicoes").resolve("senado-990006").resolve("index.html"),
                StandardCharsets.UTF_8);

        // Nome da autora da camada Silver Senado
        assertThat(html).contains("Maria Fontes IT");
        // Link para perfil da parlamentar no Senado
        assertThat(html).contains("/stat-parlamentares/senado-9999/");
        // Partido/UF
        assertThat(html).contains("MDB/RJ");
    }

    // ── JSON embutido ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("script __proposicao_data__ contém os dados corretos para hidratação Vue")
    void jsonEmbutidoRefleteDadosDoBanco() throws IOException {
        proposicaoRepository.save(Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PL")
                .numero(9907)
                .ano(2024)
                .ementa("Ementa json embutido IT.")
                .situacao("Em tramitação")
                .dataApresentacao(LocalDate.of(2024, 5, 1))
                .idOrigem("990007")
                .build());

        pageGeneratorService.generateAll();

        String html = Files.readString(
                tempOutputDir.resolve("stat-proposicoes").resolve("camara-990007").resolve("index.html"),
                StandardCharsets.UTF_8);

        // Script de hidratação presente no HTML
        assertThat(html).contains("id=\"__proposicao_data__\"");
        // Campos esperados no JSON embutido
        assertThat(html).contains("\"casa\":\"camara\"");
        assertThat(html).contains("\"idOriginal\":\"990007\"");
        assertThat(html).contains("\"siglaTipo\":\"PL\"");
        assertThat(html).contains("\"ano\":2024");
        assertThat(html).contains("\"ementa\":\"Ementa json embutido IT.\"");
    }

    // ── Sitemap ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sitemap-proposicoes.xml contém URL, changefreq, lastmod e priority corretos")
    void geraSitemapComConteudoCorreto() throws IOException {
        proposicaoRepository.save(Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PL")
                .numero(9908)
                .ano(2024)
                .ementa("Proposta para teste de sitemap.")
                .situacao("Aprovada")
                .dataApresentacao(LocalDate.of(2024, 3, 1))
                .idOrigem("990008")
                .virouLei(true)
                .build());

        pageGeneratorService.generateAll();

        Path sitemap = tempOutputDir.resolve("sitemap-proposicoes.xml");
        assertThat(sitemap).exists();

        String xml = Files.readString(sitemap, StandardCharsets.UTF_8);
        assertThat(xml).contains("<urlset");
        assertThat(xml).contains("translegis.com.br");
        assertThat(xml).contains("990008");
        // Frequência de atualização
        assertThat(xml).contains("<changefreq>weekly</changefreq>");
        // Tag lastmod presente (valor é a data de dagens/atualização via
        // @UpdateTimestamp)
        assertThat(xml).contains("<lastmod>");
        // Priority 0.7 para proposições da Câmara
        assertThat(xml).contains("<priority>0.7</priority>");
    }

    // ── Edge case ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("retorna zero quando não há proposições no banco")
    void retornaZeroSemProposicoes() {
        int count = pageGeneratorService.generateAll();
        assertThat(count).isZero();
    }
}
