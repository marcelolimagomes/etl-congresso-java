package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.pagegen.dto.DeputadoPageDTO;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ParlamentaresPageGeneratorService — geração de páginas estáticas")
@SuppressWarnings("null")
class ParlamentaresPageGeneratorServiceTest {

    @Mock
    SilverCamaraDeputadoRepository deputadoRepository;
    @Mock
    SilverSenadoSenadorRepository senadorRepository;
    @Mock
    SilverCamaraDeputadoOrgaoRepository deputadoOrgaoRepository;
    @Mock
    SilverCamaraDeputadoFrenteRepository deputadoFrenteRepository;
    @Mock
    SilverCamaraDespesaRepository despesaRepository;
    @Mock
    SilverCamaraProposicaoAutorRepository proposicaoAutorRepository;
    @Mock
    SilverCamaraProposicaoRepository silverCamaraProposicaoRepository;
    @Mock
    SilverSenadoAutoriaRepository senadoAutoriaRepository;
    @Mock
    SilverSenadoRelatoriaRepository senadoRelatoriaRepository;
    @Mock
    ProposicaoRepository proposicaoRepository;
    @Mock
    ThymeleafRenderer renderer;
    @Mock
    ParlamentaresSitemapGenerator sitemapGenerator;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    ParlamentaresPageGeneratorService service;

        private void mockDependenciasAuxiliaresVazias() {
            lenient().when(deputadoOrgaoRepository.findByIdDeputadoOrderByDataInicioDesc(anyString(), any(PageRequest.class)))
            .thenReturn(List.of());
            lenient().when(deputadoFrenteRepository.findByIdDeputadoOrderByIdLegislaturaDescTituloAsc(anyString(), any(PageRequest.class)))
            .thenReturn(List.of());
            lenient().when(despesaRepository.findByCamaraDeputadoId(anyString())).thenReturn(List.of());
            lenient().when(proposicaoAutorRepository.findByDeputadoAutorWithProposicao(anyString())).thenReturn(List.of());
            lenient().when(silverCamaraProposicaoRepository.findAllByCamaraIdIn(any())).thenReturn(List.of());
            lenient().when(senadoAutoriaRepository.findByCodigoParlamentarOrderByMateriaDesc(anyString())).thenReturn(List.of());
            lenient().when(senadoRelatoriaRepository.findByCodigoParlamentarOrderByMateriaDesc(anyLong())).thenReturn(List.of());
            lenient().when(proposicaoRepository.findByCasaAndIdOrigem(any(), anyString())).thenReturn(java.util.Optional.empty());
        }

    @Nested
    @DisplayName("slugs de saída")
    class OutputPaths {

        @Test
        @DisplayName("gera slug de deputado no formato usado pelo frontend")
        void deputadoSlug() {
            assertThat(ParlamentaresPageGeneratorService.buildDeputadoOutputPath("204554"))
                    .isEqualTo("stat-parlamentares/camara-204554");
        }

        @Test
        @DisplayName("gera slug de senador no formato usado pelo frontend")
        void senadorSlug() {
            assertThat(ParlamentaresPageGeneratorService.buildSenadorOutputPath("5529"))
                    .isEqualTo("stat-parlamentares/senado-5529");
        }
    }

    @Nested
    @DisplayName("generateAll")
    class GenerateAll {

        @Test
        @DisplayName("gera arquivos HTML em paths compatíveis com o frontend")
        void geraArquivosEmPathsCompativeis(@TempDir Path dir) throws IOException {
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "batchSize", 100);

            var deputado = SilverCamaraDeputado.builder()
                    .camaraId("204554")
                    .nomeParlamentar("Maria Teste")
                    .detStatusSiglaPartido("PT")
                    .detStatusSiglaUf("SP")
                    .build();
            var senador = SilverSenadoSenador.builder()
                    .codigoSenador("5529")
                    .nomeParlamentar("Carlos Teste")
                    .siglaPartidoParlamentar("PSD")
                    .ufParlamentar("MG")
                    .build();

            when(deputadoRepository.count()).thenReturn(1L);
            when(senadorRepository.count()).thenReturn(1L);
            when(deputadoRepository.findAll(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.<SilverCamaraDeputado>of(deputado), PageRequest.of(0, 100), 1));
            when(senadorRepository.findAll(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.<SilverSenadoSenador>of(senador), PageRequest.of(0, 100), 1));
                when(deputadoRepository.findAll()).thenReturn(List.of(deputado));
                when(senadorRepository.findAll()).thenReturn(List.of(senador));
                mockDependenciasAuxiliaresVazias();
            when(renderer.render(any(), any())).thenReturn("<html/>");

            int[] total = service.generateAll();

            assertThat(total).containsExactly(1, 1);
            assertThat(dir.resolve("stat-parlamentares/camara-204554/index.html")).exists();
            assertThat(dir.resolve("stat-parlamentares/senado-5529/index.html")).exists();
            assertThat(dir.resolve("parlamentares/deputados/204554/index.html")).doesNotExist();
            assertThat(dir.resolve("parlamentares/senadores/5529/index.html")).doesNotExist();
            Path indexFile = dir.resolve("stat-parlamentares/index.html");
            assertThat(indexFile).exists();
            String indexHtml = java.nio.file.Files.readString(indexFile);
            assertThat(indexHtml).contains("google-adsense-account");
            assertThat(indexHtml).contains("pagead2.googlesyndication.com/pagead/js/adsbygoogle.js");
            assertThat(indexHtml).contains("www.googletagmanager.com/gtag/js?id=G-RR9S2KQ44D");
            assertThat(indexHtml).contains("gtag('config', 'G-RR9S2KQ44D')");
        }

        @Test
        @DisplayName("retorna zero quando não há parlamentares")
        void retornaZeroSemDados(@TempDir Path dir) {
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "batchSize", 100);

            when(deputadoRepository.count()).thenReturn(0L);
            when(senadorRepository.count()).thenReturn(0L);

            int[] total = service.generateAll();

            assertThat(total).containsExactly(0, 0);
            assertThat(dir.resolve("parlamentares")).doesNotExist();
        }

        @Test
        @DisplayName("renderiza um template para cada parlamentar encontrado")
        void renderizaPorRegistro(@TempDir Path dir) {
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(Objects.requireNonNull(service), "batchSize", 100);

            var deputado = SilverCamaraDeputado.builder().camaraId("100").nomeParlamentar("Dep Teste").build();
            var senador = SilverSenadoSenador.builder().codigoSenador("200").nomeParlamentar("Sen Teste").build();

            when(deputadoRepository.count()).thenReturn(1L);
            when(senadorRepository.count()).thenReturn(1L);
            when(deputadoRepository.findAll(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.<SilverCamaraDeputado>of(deputado), PageRequest.of(0, 100), 1));
            when(senadorRepository.findAll(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.<SilverSenadoSenador>of(senador), PageRequest.of(0, 100), 1));
                when(deputadoRepository.findAll()).thenReturn(List.of(deputado));
                when(senadorRepository.findAll()).thenReturn(List.of(senador));
                mockDependenciasAuxiliaresVazias();
            when(renderer.render(any(), any())).thenReturn("<html/>");

            service.generateAll();

            verify(renderer, times(2)).render(any(), any());
        }

    @Test
    @DisplayName("inclui JSON-LD de Person e BreadcrumbList para deputado")
    void incluiJsonLdParaDeputado(@TempDir Path dir) {
        ReflectionTestUtils.setField(Objects.requireNonNull(service), "outputDirConfig", dir.toString());
        ReflectionTestUtils.setField(Objects.requireNonNull(service), "batchSize", 100);

        var deputado = SilverCamaraDeputado.builder()
            .camaraId("204554")
            .nomeParlamentar("Maria Teste")
            .sexo("F")
            .detStatusSiglaPartido("PT")
            .detStatusSiglaUf("SP")
            .detGabineteEmail("maria@camara.leg.br")
            .detStatusUrlFoto("https://img.camara/maria.jpg")
            .urlWebsite("https://maria.example")
            .detRedeSocial("[\"https://instagram.com/maria\"]")
            .build();

        when(deputadoRepository.count()).thenReturn(1L);
        when(senadorRepository.count()).thenReturn(0L);
        when(deputadoRepository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(List.of(deputado), PageRequest.of(0, 100), 1));
        when(deputadoRepository.findAll()).thenReturn(List.of(deputado));
        when(senadorRepository.findAll()).thenReturn(List.of());
        mockDependenciasAuxiliaresVazias();
        when(renderer.render(any(), any())).thenReturn("<html/>");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);

        service.generateAll();

        verify(renderer).render(eq("parlamentares/deputado"), modelCaptor.capture());
        DeputadoPageDTO page = (DeputadoPageDTO) modelCaptor.getValue().get("page");
        assertThat(page.getSchemaOrgPersonJson()).contains("\"@type\":\"Person\"");
        assertThat(page.getSchemaOrgPersonJson()).contains("Maria Teste");
        assertThat(page.getSchemaOrgPersonJson()).contains("maria@camara.leg.br");
        assertThat(page.getEmail()).isEqualTo("maria@camara.leg.br");
        assertThat(page.getSchemaOrgBreadcrumbJson()).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(page.getSchemaOrgBreadcrumbJson()).contains("/stat-parlamentares/");
    }

    @Test
    @DisplayName("inclui JSON-LD de Person e BreadcrumbList para senador")
    void incluiJsonLdParaSenador(@TempDir Path dir) {
        ReflectionTestUtils.setField(Objects.requireNonNull(service), "outputDirConfig", dir.toString());
        ReflectionTestUtils.setField(Objects.requireNonNull(service), "batchSize", 100);

        var senador = SilverSenadoSenador.builder()
            .codigoSenador("5529")
            .nomeParlamentar("Carlos Teste")
            .sexo("M")
            .siglaPartidoParlamentar("PSD")
            .ufParlamentar("MG")
            .detContatoEmail("carlos@senado.leg.br")
            .detUrlFoto("https://img.senado/carlos.jpg")
            .detUrlPaginaParlamentar("https://senado.leg.br/carlos")
            .detTwitter("https://twitter.com/carlos")
            .build();

        when(deputadoRepository.count()).thenReturn(0L);
        when(senadorRepository.count()).thenReturn(1L);
        when(deputadoRepository.findAll()).thenReturn(List.of());
        when(senadorRepository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(List.of(senador), PageRequest.of(0, 100), 1));
        when(senadorRepository.findAll()).thenReturn(List.of(senador));
        mockDependenciasAuxiliaresVazias();
        when(renderer.render(any(), any())).thenReturn("<html/>");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);

        service.generateAll();

        verify(renderer).render(eq("parlamentares/senador"), modelCaptor.capture());
        SenadorPageDTO page = (SenadorPageDTO) modelCaptor.getValue().get("page");
        assertThat(page.getSchemaOrgPersonJson()).contains("\"@type\":\"Person\"");
        assertThat(page.getSchemaOrgPersonJson()).contains("Carlos Teste");
        assertThat(page.getSchemaOrgBreadcrumbJson()).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(page.getSchemaOrgBreadcrumbJson()).contains("/stat-parlamentares/");
    }
    }

    @Nested
    @DisplayName("generateIndex")
    class GenerateIndex {

        @Test
        @DisplayName("gera page-1.json e meta.json com dados de deputados e senadores")
        void geraArquivosDeIndice(@TempDir Path dir) throws IOException {
            var deputado = SilverCamaraDeputado.builder()
                    .camaraId("204554")
                    .nomeParlamentar("Maria Teste")
                    .detStatusSiglaPartido("PT")
                    .detStatusSiglaUf("SP")
                    .detStatusUrlFoto("https://img.camara/maria.jpg")
                    .build();
            var senador = SilverSenadoSenador.builder()
                    .codigoSenador("5529")
                    .nomeParlamentar("Carlos Teste")
                    .siglaPartidoParlamentar("PSD")
                    .ufParlamentar("MG")
                    .participacao("T")
                    .detUrlFoto("https://img.senado/carlos.jpg")
                    .build();

            when(deputadoRepository.findAll()).thenReturn(List.of(deputado));
            when(senadorRepository.findAll()).thenReturn(List.of(senador));

            service.generateIndex(dir);

            Path page1 = dir.resolve("parlamentares/indice/data/page-1.json");
            Path meta = dir.resolve("parlamentares/indice/data/meta.json");
            String page1Content = java.nio.file.Files.readString(page1);
            String metaContent = java.nio.file.Files.readString(meta);

            assertThat(page1).exists();
            assertThat(meta).exists();
            assertThat(page1Content).contains("camara-204554");
            assertThat(page1Content).contains("senado-5529");
            assertThat(page1Content).contains("/stat-parlamentares/camara-204554/");
            assertThat(page1Content).contains("/stat-parlamentares/senado-5529/");
            assertThat(metaContent).contains("\"totalDeputados\":1");
            assertThat(metaContent).contains("\"totalSenadores\":1");
        }

        @Test
        @DisplayName("não gera arquivos quando não há parlamentares")
        void naoGeraArquivosSemDados(@TempDir Path dir) {
            when(deputadoRepository.findAll()).thenReturn(List.of());
            when(senadorRepository.findAll()).thenReturn(List.of());

            service.generateIndex(dir);

            assertThat(dir.resolve("parlamentares/indice/data")).doesNotExist();
        }
    }
}