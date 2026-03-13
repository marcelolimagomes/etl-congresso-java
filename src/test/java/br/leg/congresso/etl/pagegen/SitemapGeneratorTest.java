package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.repository.ProposicaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SitemapGenerator — geração de sitemap XML")
class SitemapGeneratorTest {

    @Mock
    ProposicaoRepository proposicaoRepository;

    @InjectMocks
    SitemapGenerator generator;

    @Nested
    @DisplayName("Arquivo gerado")
    class ArquivoGerado {

        @Test
        @DisplayName("cria sitemap-proposicoes.xml no diretório base")
        void criaArquivo(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of());
            generator.generate(dir);
            assertThat(dir.resolve("sitemap-proposicoes.xml")).exists();
        }

        @Test
        @DisplayName("arquivo começa com declaração XML e namespace do sitemaps.org")
        void xmlValido(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of());
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).startsWith("<?xml");
            assertThat(xml).contains("sitemaps.org");
        }
    }

    @Nested
    @DisplayName("URLs no sitemap")
    class Urls {

        @Test
        @DisplayName("URL de proposição da Câmara contém /stat-proposicoes/camara-{id}")
        void urlCamara(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(makeProposicao(CasaLegislativa.CAMARA, "2342835")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).contains("https://www.translegis.com.br/stat-proposicoes/camara-2342835/");
        }

        @Test
        @DisplayName("URL de proposição do Senado contém /stat-proposicoes/senado-{id}")
        void urlSenado(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(makeProposicao(CasaLegislativa.SENADO, "12345")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).contains("https://www.translegis.com.br/stat-proposicoes/senado-12345/");
        }

        @Test
        @DisplayName("múltiplas proposições geram múltiplos <url> no sitemap")
        void multiplosUrls(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(
                    makeProposicao(CasaLegislativa.CAMARA, "1"),
                    makeProposicao(CasaLegislativa.SENADO, "2"),
                    makeProposicao(CasaLegislativa.CAMARA, "3")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            long count = xml.lines().filter(l -> l.contains("<loc>")).count();
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("sitemap vazio quando não há proposições")
        void sitemapVazio(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of());
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).doesNotContain("<loc>");
        }
    }

    @Nested
    @DisplayName("Atributos de prioridade")
    class Prioridade {

        @Test
        @DisplayName("proposição da Câmara tem priority 0.7")
        void prioridadeCamara(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(makeProposicao(CasaLegislativa.CAMARA, "1")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).contains("<priority>0.7</priority>");
        }

        @Test
        @DisplayName("proposição do Senado tem priority 0.6")
        void prioridadeSenado(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(makeProposicao(CasaLegislativa.SENADO, "1")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).contains("<priority>0.6</priority>");
        }

        @Test
        @DisplayName("changefreq é weekly")
        void changefreqWeekly(@TempDir Path dir) throws IOException {
            mockProposicoes(List.of(makeProposicao(CasaLegislativa.CAMARA, "1")));
            generator.generate(dir);
            String xml = Files.readString(dir.resolve("sitemap-proposicoes.xml"));
            assertThat(xml).contains("<changefreq>weekly</changefreq>");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void mockProposicoes(List<ProposicaoRepository.SitemapProjection> proposicoes) {
        when(proposicaoRepository.count()).thenReturn((long) proposicoes.size());
        if (!proposicoes.isEmpty()) {
            when(proposicaoRepository.findAllForSitemap(any()))
                    .thenReturn(new PageImpl<>(proposicoes, PageRequest.of(0, 1000), proposicoes.size()));
        }
    }

    private ProposicaoRepository.SitemapProjection makeProposicao(CasaLegislativa casa, String idOrigem) {
        return new ProposicaoRepository.SitemapProjection() {
            @Override
            public CasaLegislativa getCasa() {
                return casa;
            }

            @Override
            public String getIdOrigem() {
                return idOrigem;
            }

            @Override
            public java.time.LocalDateTime getAtualizadoEm() {
                return java.time.LocalDateTime.now();
            }
        };
    }
}
