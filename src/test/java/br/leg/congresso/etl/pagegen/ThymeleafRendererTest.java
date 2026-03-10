package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.leg.congresso.etl.pagegen.dto.AutorDTO;
import br.leg.congresso.etl.pagegen.dto.ProposicaoPageDTO;
import br.leg.congresso.etl.pagegen.dto.TramitacaoDTO;

@DisplayName("ThymeleafRenderer — renderização de template proposicao")
class ThymeleafRendererTest {

    private ThymeleafRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafRenderer();
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    private ProposicaoPageDTO buildPage() {
        return ProposicaoPageDTO.builder()
                .casa("camara")
                .casaLabel("Câmara dos Deputados")
                .idOriginal("2342835")
                .siglaTipo("PL")
                .descricaoTipo("Projeto de Lei")
                .numero("1234")
                .ano(2024)
                .ementa("Dispõe sobre acesso à informação legislativa.")
                .ementaDetalhada("Ementa detalhada com indexação técnica.")
                .situacaoDescricao("Em tramitação")
                .situacaoTramitacao("tramitando")
                .dataApresentacao("15/03/2024")
                .orgaoAtual("CCJ")
                .regime("Ordinária")
                .urlInteiroTeor("https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2342835")
                .keywords(List.of("acesso à informação", "transparência"))
                .temas(List.of("Administração Pública"))
                .autoriaResumo("Dep. João Silva (PT/SP)")
                .autores(List.of(
                        AutorDTO.builder()
                                .nome("João Silva")
                                .tipo("Deputado Federal")
                                .casa("camara")
                                .idOriginal("204560")
                                .proponente(true)
                                .partido("PT")
                                .uf("SP")
                                .build()))
                .tramitacoes(List.of(
                        TramitacaoDTO.builder()
                                .dataFormatada("15/03/2024")
                                .siglaOrgao("MESA")
                                .descricaoOrgao("Mesa Diretora")
                                .descricao("Proposição recebida pela Mesa Diretora.")
                                .situacao("Em tramitação")
                                .despacho("Encaminhar à CCJ para análise.")
                                .build()))
                .canonicalUrl("https://www.translegis.com.br/proposicoes/camara-2342835")
                .seoTitle("PL 1234/2024 — Câmara dos Deputados | Transparência Legislativa")
                .seoDescription("Dispõe sobre acesso à informação legislativa.")
                .schemaOrgLegislationJson("{\"@context\":\"https://schema.org\",\"@type\":\"Legislation\"}")
                .schemaOrgBreadcrumbJson("{\"@context\":\"https://schema.org\",\"@type\":\"BreadcrumbList\"}")
                .dataPublicacaoIso("2024-03-15")
                .dataAtualizacaoIso("2024-06-01")
                .geradoEm("2024-06-01T12:00:00")
                .build();
    }

    // ── Básico ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("render retorna HTML não vazio")
    void render_retornaHtmlNaoVazio() {
        var html = renderer.render("proposicao", Map.of("page", buildPage()));
        assertThat(html).isNotBlank();
    }

    // ── Metadados SEO ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Metadados SEO no <head>")
    class SeoHead {

        @Test
        @DisplayName("contém <title> com seoTitle")
        void contemTitle() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("PL 1234/2024 — Câmara dos Deputados | Transparência Legislativa");
        }

        @Test
        @DisplayName("contém meta description")
        void contemMetaDescription() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("Dispõe sobre acesso à informação legislativa.");
            assertThat(html).contains("name=\"description\"");
        }

        @Test
        @DisplayName("contém canonical URL")
        void contemCanonicalUrl() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("https://www.translegis.com.br/proposicoes/camara-2342835");
            assertThat(html).contains("rel=\"canonical\"");
        }

        @Test
        @DisplayName("contém Open Graph tags")
        void contemOpenGraph() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("og:type");
            assertThat(html).contains("og:title");
            assertThat(html).contains("og:description");
        }

        @Test
        @DisplayName("contém JSON-LD de Legislation")
        void contemJsonLdLegislation() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("\"@type\":\"Legislation\"");
        }

        @Test
        @DisplayName("contém JSON-LD de BreadcrumbList")
        void contemJsonLdBreadcrumb() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        }
    }

    // ── Conteúdo principal ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Conteúdo do corpo da página")
    class Conteudo {

        @Test
        @DisplayName("contém sigla + número + ano em h1")
        void contemH1ComIdentificacao() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("<h1");
            assertThat(html).contains("PL 1234/2024");
        }

        @Test
        @DisplayName("contém texto da ementa")
        void contemEmenta() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("Dispõe sobre acesso à informação legislativa.");
        }

        @Test
        @DisplayName("contém badge da casa legislativa")
        void contemBadgeCasa() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("badge-camara");
            assertThat(html).contains("Câmara dos Deputados");
        }

        @Test
        @DisplayName("contém link para inteiro teor")
        void contemLinkInteiroTeor() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("Inteiro Teor");
            assertThat(html).contains("fichadetramitacao");
        }

        @Test
        @DisplayName("contém seção de informações gerais")
        void contemInformacoesGerais() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("Informações Gerais");
            assertThat(html).contains("Projeto de Lei");
        }
    }

    // ── Autores ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Seção de autores")
    class Autores {

        @Test
        @DisplayName("contém nome do autor no HTML")
        void contemNomeAutor() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("João Silva");
        }

        @Test
        @DisplayName("contém link para perfil do parlamentar na câmara")
        void contemLinkParlamentarCamara() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("/parlamentares/camara-204560");
        }

        @Test
        @DisplayName("contém partido e UF do autor")
        void contemPartidoUf() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("PT");
            assertThat(html).contains("SP");
        }

        @Test
        @DisplayName("sem autores: seção de autores não é renderizada")
        void semAutores_secaoNaoRenderizada() {
            var page = ProposicaoPageDTO.builder()
                    .casa("senado").casaLabel("Senado Federal")
                    .idOriginal("12345").siglaTipo("PL").descricaoTipo("Projeto de Lei")
                    .numero("100").ano(2023)
                    .ementa("Ementa teste.")
                    .situacaoTramitacao("tramitando")
                    .seoTitle("PL 100/2023").seoDescription("Ementa teste.")
                    .canonicalUrl("https://x.com/proposicoes/senado-12345")
                    .schemaOrgLegislationJson("{}")
                    .schemaOrgBreadcrumbJson("{}")
                    .geradoEm("2024-01-01T00:00:00")
                    .build();
            var html = renderer.render("proposicao", Map.of("page", page));
            // sem autores: o data-testid do card de autores não deve aparecer
            assertThat(html).doesNotContain("proposicao-tab-autores");
        }
    }

    // ── Tramitação ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Seção de tramitação")
    class Tramitacao {

        @Test
        @DisplayName("contém data formatada da tramitação")
        void contemDataFormatada() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("15/03/2024");
        }

        @Test
        @DisplayName("contém sigla do órgão como badge")
        void contemSiglaOrgao() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("MESA");
        }

        @Test
        @DisplayName("contém despacho em itálico")
        void contemDespacho() {
            var html = renderer.render("proposicao", Map.of("page", buildPage()));
            assertThat(html).contains("Encaminhar à CCJ para análise.");
        }

        @Test
        @DisplayName("sem tramitações: seção de tramitação não é renderizada")
        void semTramitacoes_secaoNaoRenderizada() {
            var page = ProposicaoPageDTO.builder()
                    .casa("camara").casaLabel("Câmara dos Deputados")
                    .idOriginal("1").siglaTipo("PEC").descricaoTipo("Proposta de Emenda à Constituição")
                    .numero("1").ano(2024)
                    .ementa("Ementa PEC.")
                    .situacaoTramitacao("tramitando")
                    .seoTitle("PEC 1/2024").seoDescription("Ementa.")
                    .canonicalUrl("https://x.com/proposicoes/camara-1")
                    .schemaOrgLegislationJson("{}")
                    .schemaOrgBreadcrumbJson("{}")
                    .geradoEm("2024-01-01T00:00:00")
                    .build();
            var html = renderer.render("proposicao", Map.of("page", page));
            assertThat(html).doesNotContain("proposicao-tab-tramitacao");
        }
    }

    // ── Senado ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("casa senado gera badge-senado")
    void senadoGeraBadgeSenado() {
        var page = ProposicaoPageDTO.builder()
                .casa("senado").casaLabel("Senado Federal")
                .idOriginal("12345").siglaTipo("PLS").descricaoTipo("Projeto de Lei do Senado")
                .numero("50").ano(2023)
                .ementa("Ementa PLS.")
                .situacaoTramitacao("tramitando")
                .seoTitle("PLS 50/2023").seoDescription("Ementa PLS.")
                .canonicalUrl("https://x.com/proposicoes/senado-12345")
                .schemaOrgLegislationJson("{}")
                .schemaOrgBreadcrumbJson("{}")
                .geradoEm("2024-01-01T00:00:00")
                .build();
        var html = renderer.render("proposicao", Map.of("page", page));
        assertThat(html).contains("badge-senado");
        assertThat(html).contains("Senado Federal");
    }

    // ── Breadcrumb ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("breadcrumb contém link para /proposicoes")
    void breadcrumbContemLinkProposicoes() {
        var html = renderer.render("proposicao", Map.of("page", buildPage()));
        assertThat(html).contains("href=\"/proposicoes\"");
        assertThat(html).contains("Proposições");
    }

    @Test
    @DisplayName("breadcrumb exibe sigla/número/ano da proposição")
    void breadcrumbExibeIdentificacao() {
        var html = renderer.render("proposicao", Map.of("page", buildPage()));
        // nav.breadcrumb contém o nome legível da proposição
        assertThat(html).contains("PL 1234/2024");
    }
}
