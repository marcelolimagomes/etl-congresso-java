package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("ParlamentaresPageGenerationIT — pipeline completo Silver → HTML")
class ParlamentaresPageGenerationIT {

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
    private ParlamentaresPageGeneratorService pageGeneratorService;

    @Autowired
    private SilverCamaraDeputadoRepository silverCamaraDeputadoRepository;

    @Autowired
    private SilverSenadoSenadorRepository silverSenadoSenadorRepository;

    private Path tempOutputDir;

    @BeforeEach
    void setUp() throws IOException {
        tempOutputDir = Files.createTempDirectory("parlamentares-pagegen-it");
        ReflectionTestUtils.setField(Objects.requireNonNull(pageGeneratorService), "outputDirConfig", tempOutputDir.toString());
        ReflectionTestUtils.setField(Objects.requireNonNull(pageGeneratorService), "batchSize", 100);
        limpaBanco();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempOutputDir);
        limpaBanco();
    }

    private void limpaBanco() {
        silverCamaraDeputadoRepository.deleteAll();
        silverSenadoSenadorRepository.deleteAll();
    }

    @Test
    @DisplayName("gera HTML de deputado em rota compatível com o frontend")
    void geraHtmlDeputado() throws IOException {
        SilverCamaraDeputado deputado = SilverCamaraDeputado.builder()
                .ingeridoEm(LocalDateTime.now())
                .camaraId("204554")
                .nomeParlamentar("Maria da Silva")
                .nomeCivil("Maria da Silva")
                .sexo("F")
                .detStatusSiglaPartido("PT")
                .detStatusSiglaUf("SP")
                .detStatusSituacao("Em exercício")
                .detGabineteEmail("maria@camara.leg.br")
                .detStatusUrlFoto("https://img.camara.leg.br/maria.jpg")
                .origemCarga("CSV")
                .build();
            silverCamaraDeputadoRepository.save(Objects.requireNonNull(deputado));

        int[] total = pageGeneratorService.generateAll();

        assertThat(total).containsExactly(1, 0);

        Path htmlFile = tempOutputDir.resolve("parlamentares/camara-204554/index.html");
        assertThat(htmlFile).exists();
        assertThat(tempOutputDir.resolve("parlamentares/deputados/204554/index.html")).doesNotExist();

        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        assertThat(html).contains("Maria da Silva");
        assertThat(html).contains("maria@camara.leg.br");
        assertThat(html).contains("https://www.translegis.com.br/parlamentares/camara-204554");
        assertThat(html).contains("href=\"/parlamentares/\"");
        assertThat(html).contains("application/ld+json");
        assertThat(html).contains("\"@type\":\"Person\"");
        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(tempOutputDir.resolve("parlamentares/indice/data/page-1.json")).exists();
        assertThat(tempOutputDir.resolve("parlamentares/indice/data/meta.json")).exists();
        Path sitemap = tempOutputDir.resolve("sitemap-parlamentares.xml");
        assertThat(sitemap).exists();
        assertThat(Files.readString(sitemap, StandardCharsets.UTF_8))
            .contains("https://www.translegis.com.br/parlamentares/camara-204554");
    }

    @Test
    @DisplayName("gera HTML de senador em rota compatível com o frontend")
    void geraHtmlSenador() throws IOException {
        SilverSenadoSenador senador = SilverSenadoSenador.builder()
                .ingeridoEm(LocalDateTime.now())
                .codigoSenador("5529")
                .nomeParlamentar("Carlos Souza")
                .detNomeCompleto("Carlos Souza")
                .sexo("M")
                .siglaPartidoParlamentar("PSD")
                .ufParlamentar("MG")
                .participacao("T")
                .detContatoEmail("carlos@senado.leg.br")
                .detUrlFoto("https://img.senado.leg.br/carlos.jpg")
                .origemCarga("API")
                .build();
            silverSenadoSenadorRepository.save(Objects.requireNonNull(senador));

        int[] total = pageGeneratorService.generateAll();

        assertThat(total).containsExactly(0, 1);

        Path htmlFile = tempOutputDir.resolve("parlamentares/senado-5529/index.html");
        assertThat(htmlFile).exists();
        assertThat(tempOutputDir.resolve("parlamentares/senadores/5529/index.html")).doesNotExist();

        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        assertThat(html).contains("Carlos Souza");
        assertThat(html).contains("https://www.translegis.com.br/parlamentares/senado-5529");
        assertThat(html).contains("href=\"/parlamentares/\"");
        assertThat(html).contains("application/ld+json");
        assertThat(html).contains("\"@type\":\"Person\"");
        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(tempOutputDir.resolve("parlamentares/indice/data/page-1.json")).exists();
        assertThat(tempOutputDir.resolve("parlamentares/indice/data/meta.json")).exists();
        Path sitemap = tempOutputDir.resolve("sitemap-parlamentares.xml");
        assertThat(sitemap).exists();
        assertThat(Files.readString(sitemap, StandardCharsets.UTF_8))
            .contains("https://www.translegis.com.br/parlamentares/senado-5529");
    }
}