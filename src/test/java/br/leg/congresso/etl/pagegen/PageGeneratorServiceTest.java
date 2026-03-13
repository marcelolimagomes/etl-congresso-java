package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.pagegen.dto.ProposicaoPageDTO;
import br.leg.congresso.etl.repository.ProposicaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PageGeneratorService — geração de páginas estáticas")
class PageGeneratorServiceTest {

    @Mock
    ProposicaoRepository proposicaoRepository;
    @Mock
    ProposicaoPageAssembler assembler;
    @Mock
    ThymeleafRenderer renderer;
    @Mock
    SitemapGenerator sitemapGenerator;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    PageGeneratorService service;

    @Nested
    @DisplayName("writeHtml")
    class WriteHtml {

        @Test
        @DisplayName("cria diretório e arquivo index.html")
        void criaArquivo(@TempDir Path dir) throws IOException {
            service.writeHtml(dir, "camara", "2342835", "<html>Teste</html>");
            Path arquivo = dir.resolve("stat-proposicoes/camara-2342835/index.html");
            assertThat(arquivo).exists();
            assertThat(Files.readString(arquivo)).isEqualTo("<html>Teste</html>");
        }

        @Test
        @DisplayName("sobrescreve arquivo existente")
        void sobrescreve(@TempDir Path dir) throws IOException {
            service.writeHtml(dir, "camara", "1", "<html>v1</html>");
            service.writeHtml(dir, "camara", "1", "<html>v2</html>");
            Path arquivo = dir.resolve("stat-proposicoes/camara-1/index.html");
            assertThat(Files.readString(arquivo)).isEqualTo("<html>v2</html>");
        }

        @Test
        @DisplayName("cria diretório pai automaticamente")
        void criaDiretorioPai(@TempDir Path dir) throws IOException {
            service.writeHtml(dir, "senado", "99999", "<html/>");
            assertThat(dir.resolve("stat-proposicoes/senado-99999")).isDirectory();
        }
    }

    @Nested
    @DisplayName("generateAll")
    class GenerateAll {

        @Test
        @DisplayName("processa todos os registros e gera sitemap")
        void processaTodosEGeraSitemap(@TempDir Path dir) throws IOException {
            ReflectionTestUtils.setField(service, "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(service, "batchSize", 100);

            var p1 = makeProposicao("camara", "111");
            var p2 = makeProposicao("senado", "222");

            when(proposicaoRepository.count()).thenReturn(2L);
            when(proposicaoRepository.findAllForPageGen(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 100), 2));

            when(assembler.assemble(p1)).thenReturn(makeDto("camara", "111"));
            when(assembler.assemble(p2)).thenReturn(makeDto("senado", "222"));
            when(renderer.render(any(), any())).thenReturn("<html/>");

            int total = service.generateAll();

            assertThat(total).isEqualTo(2);
            verify(sitemapGenerator).generate(dir);
            assertThat(dir.resolve("stat-proposicoes/camara-111/index.html")).exists();
            assertThat(dir.resolve("stat-proposicoes/senado-222/index.html")).exists();
            Path indexFile = dir.resolve("stat-proposicoes/index.html");
            assertThat(indexFile).exists();
            String indexHtml = Files.readString(indexFile);
            assertThat(indexHtml).contains("google-adsense-account");
            assertThat(indexHtml).contains("pagead2.googlesyndication.com/pagead/js/adsbygoogle.js");
            assertThat(indexHtml).contains("www.googletagmanager.com/gtag/js?id=G-RR9S2KQ44D");
            assertThat(indexHtml).contains("gtag('config', 'G-RR9S2KQ44D')");
        }

        @Test
        @DisplayName("retorna zero quando não há proposições")
        void retornaZeroSemProposicoes(@TempDir Path dir) throws IOException {
            ReflectionTestUtils.setField(service, "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(service, "batchSize", 100);

            when(proposicaoRepository.count()).thenReturn(0L);

            int total = service.generateAll();

            assertThat(total).isEqualTo(0);
            verify(sitemapGenerator).generate(dir);
        }

        @Test
        @DisplayName("processa múltiplos batches sequencialmente")
        void processaMultiplosBatches(@TempDir Path dir) throws IOException {
            ReflectionTestUtils.setField(service, "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(service, "batchSize", 1);

            var p1 = makeProposicao("camara", "1");
            var p2 = makeProposicao("camara", "2");

            when(proposicaoRepository.count()).thenReturn(2L);
            when(proposicaoRepository.findAllForPageGen(PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(List.of(p1), PageRequest.of(0, 1), 2));
            when(proposicaoRepository.findAllForPageGen(PageRequest.of(1, 1)))
                    .thenReturn(new PageImpl<>(List.of(p2), PageRequest.of(1, 1), 2));
            // generateIndex usa INDEX_PAGE_SIZE=100 independente do batchSize
            when(proposicaoRepository.findAllForPageGen(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

            when(assembler.assemble(any())).thenReturn(makeDto("camara", "x"));
            when(renderer.render(any(), any())).thenReturn("<html/>");

            int total = service.generateAll();

            assertThat(total).isEqualTo(2);
            // 2 lotes de proposições (batchSize=1) + 1 chamada do generateIndex
            // (INDEX_PAGE_SIZE=100)
            verify(proposicaoRepository, times(3)).findAllForPageGen(any());
        }
    }

    @Nested
    @DisplayName("generateAll(ano)")
    class GenerateAllByAno {

        @Test
        @DisplayName("usa countByAno e findAllForPageGenByAno quando ano é informado")
        void filtraPorAno(@TempDir Path dir) throws IOException {
            ReflectionTestUtils.setField(service, "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(service, "batchSize", 100);

            var p = makeProposicao("camara", "333");

            when(proposicaoRepository.countByAno(2024)).thenReturn(1L);
            when(proposicaoRepository.findAllForPageGenByAno(eq(2024), any()))
                    .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 100), 1));

            when(assembler.assemble(p)).thenReturn(makeDto("camara", "333"));
            when(renderer.render(any(), any())).thenReturn("<html/>");

            int total = service.generateAll(2024);

            assertThat(total).isEqualTo(1);
            verify(proposicaoRepository).countByAno(2024);
            verify(proposicaoRepository).findAllForPageGenByAno(eq(2024), any());
        }

        @Test
        @DisplayName("retorna zero quando não há proposições no ano")
        void retornaZeroAnoSemProposicoes(@TempDir Path dir) {
            ReflectionTestUtils.setField(service, "outputDirConfig", dir.toString());
            ReflectionTestUtils.setField(service, "batchSize", 100);

            when(proposicaoRepository.countByAno(2020)).thenReturn(0L);

            int total = service.generateAll(2020);

            assertThat(total).isEqualTo(0);
            verify(sitemapGenerator).generate(dir);
        }
    }

    @Nested
    @DisplayName("writeIndexJson")
    class WriteIndexJson {

        @Test
        @DisplayName("grava page-1.json em proposicoes/indice/data/")
        void pagina1(@TempDir Path dir) throws IOException {
            service.writeIndexJson(dir, 1, "{\"ok\":true}");
            Path arquivo = dir.resolve("proposicoes/indice/data/page-1.json");
            assertThat(arquivo).exists();
            assertThat(Files.readString(arquivo)).isEqualTo("{\"ok\":true}");
        }

        @Test
        @DisplayName("grava page-N.json em proposicoes/indice/data/")
        void paginaN(@TempDir Path dir) throws IOException {
            service.writeIndexJson(dir, 5, "{\"page\":5}");
            Path arquivo = dir.resolve("proposicoes/indice/data/page-5.json");
            assertThat(arquivo).exists();
            assertThat(Files.readString(arquivo)).isEqualTo("{\"page\":5}");
        }
    }

    @Nested
    @DisplayName("generateIndex")
    class GenerateIndex {

        @Test
        @DisplayName("não gera arquivos quando não há proposições")
        void semProposicoes(@TempDir Path dir) {
            when(proposicaoRepository.count()).thenReturn(0L);

            service.generateIndex(dir);

            assertThat(dir.resolve("proposicoes/indice")).doesNotExist();
        }

        @Test
        @DisplayName("gera JSON do índice para página 1 e meta.json")
        void geraIndice(@TempDir Path dir) {
            var p = makeProposicao("camara", "9999");
            when(proposicaoRepository.count()).thenReturn(1L);
            when(proposicaoRepository.findAllForPageGen(PageRequest.of(0, 100)))
                    .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 100), 1));

            service.generateIndex(dir);

            assertThat(dir.resolve("proposicoes/indice/data/page-1.json")).exists();
            assertThat(dir.resolve("proposicoes/indice/data/meta.json")).exists();
            assertThat(dir.resolve("proposicoes/indice/data/page-2.json")).doesNotExist();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Proposicao makeProposicao(String casa, String idOrigem) {
        return Proposicao.builder()
                .id(UUID.randomUUID())
                .casa("camara".equals(casa) ? CasaLegislativa.CAMARA : CasaLegislativa.SENADO)
                .tipo(TipoProposicao.LEI_ORDINARIA).sigla("PL")
                .numero(1).ano(2024).ementa("Ementa.").idOrigem(idOrigem)
                .build();
    }

    private ProposicaoPageDTO makeDto(String casa, String idOriginal) {
        return ProposicaoPageDTO.builder()
                .casa(casa)
                .casaLabel("c")
                .idOriginal(idOriginal)
                .siglaTipo("PL").descricaoTipo("Proj.").numero("1").ano(2024)
                .ementa("E.")
                .canonicalUrl("https://www.translegis.com.br/stat-proposicoes/" + casa + "-" + idOriginal + "/")
                .seoTitle("t").seoDescription("d")
                .schemaOrgLegislationJson("{}").schemaOrgBreadcrumbJson("{}")
                .geradoEm("2024-01-01")
                .build();
    }
}
