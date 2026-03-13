package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoRelacionada;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;
import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.TramitacaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRelacionadaRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoTemaRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoDocumentoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoVotacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProposicaoPageAssembler — montagem do DTO")
class ProposicaoPageAssemblerTest {

    @Mock
    ProposicaoRepository proposicaoRepository;
    @Mock
    TramitacaoRepository tramitacaoRepository;
    @Mock
    SilverCamaraProposicaoRepository silverCamaraProposicaoRepository;
    @Mock
    SilverCamaraProposicaoAutorRepository silverCamaraAutorRepository;
    @Mock
    SilverCamaraProposicaoTemaRepository silverCamaraProposicaoTemaRepository;
    @Mock
    SilverCamaraProposicaoRelacionadaRepository silverCamaraProposicaoRelacionadaRepository;
    @Mock
    SilverCamaraVotacaoRepository silverCamaraVotacaoRepository;
    @Mock
    SilverSenadoMateriaRepository silverSenadoMateriaRepository;
    @Mock
    SilverSenadoAutoriaRepository silverSenadoAutoriaRepository;
    @Mock
    SilverSenadoDocumentoRepository silverSenadoDocumentoRepository;
    @Mock
    SilverSenadoSenadorRepository silverSenadoSenadorRepository;
    @Mock
    SilverSenadoVotacaoRepository silverSenadoVotacaoRepository;

    @InjectMocks
    ProposicaoPageAssembler assembler;

    @BeforeEach
    void setUp() {
        when(silverCamaraProposicaoRelacionadaRepository.findByProposicaoId(any())).thenReturn(List.of());
        when(silverCamaraVotacaoRepository.findByUltimaApresentacaoProposicaoIdProposicaoOrderByDataDesc(any()))
            .thenReturn(List.of());
        when(silverSenadoDocumentoRepository.findBySenadoMateriaId(any())).thenReturn(List.of());
        when(silverSenadoVotacaoRepository.findBySenadoMateriaId(any())).thenReturn(List.of());
        when(silverSenadoSenadorRepository.findByCodigoSenador(any())).thenReturn(Optional.empty());
        when(proposicaoRepository.findByCasaAndIdOrigem(any(), any())).thenReturn(Optional.empty());

        // injeta ObjectMapper real para serialização JSON-LD
        var field = findField(ProposicaoPageAssembler.class, "objectMapper");
        if (field != null) {
            field.setAccessible(true);
            try {
                field.set(assembler, new ObjectMapper());
            } catch (Exception ignored) {
            }
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    // ── Câmara ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Proposição da Câmara")
    class Camara {

        private final UUID silverCamaraId = UUID.randomUUID();

        private Proposicao buildProposicaoCamara() {
            return Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.CAMARA)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PL")
                    .numero(1234)
                    .ano(2024)
                    .ementa("Dispõe sobre acesso à informação.")
                    .situacao("Em Tramitação no Senado Federal")
                    .dataApresentacao(LocalDate.of(2024, 3, 15))
                    .dataAtualizacao(LocalDateTime.of(2024, 6, 1, 10, 0))
                    .idOrigem("2342835")
                    .urlInteiroTeor("https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2342835")
                    .keywords("transparência, acesso à informação")
                    .silverCamaraId(silverCamaraId)
                    .build();
        }

        @BeforeEach
        void mockCamaraData() {
            when(tramitacaoRepository.findByProposicaoIdOrdered(any())).thenReturn(List.of(
                    Tramitacao.builder()
                            .sequencia(1)
                            .dataHora(LocalDateTime.of(2024, 3, 15, 0, 0))
                            .siglaOrgao("MESA")
                            .descricaoTramitacao("Proposição recebida pela Mesa.")
                            .build()));

                when(silverCamaraProposicaoRepository.findById(Objects.requireNonNull(silverCamaraId)))
                    .thenReturn(Optional.of(SilverCamaraProposicao.builder()
                            .id(silverCamaraId)
                            .ementaDetalhada("Ementa detalhada.")
                            .statusRegime("Ordinária")
                            .statusSiglaOrgao("CCJ")
                            .build()));

            when(silverCamaraAutorRepository.findByCamaraProposicaoIdOrderByOrdemAssinaturaAsc(silverCamaraId))
                    .thenReturn(List.of(
                            SilverCamaraProposicaoAutor.builder()
                                    .nomeAutor("João Silva")
                                    .tipoAutor("Deputado Federal")
                                    .idDeputadoAutor("204560")
                                    .siglaPartidoAutor("PT")
                                    .siglaUfAutor("SP")
                                    .proponente(1)
                                    .ordemAssinatura(1)
                                    .build()));

            when(silverCamaraProposicaoTemaRepository.findByCamaraProposicaoId(silverCamaraId))
                    .thenReturn(List.of(
                            SilverCamaraProposicaoTema.builder().tema("Administração Pública").build()));
        }

        @Test
        @DisplayName("monta DTO com identificação correta")
        void montaIdentificacao() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getCasa()).isEqualTo("camara");
            assertThat(dto.getCasaLabel()).isEqualTo("Câmara dos Deputados");
            assertThat(dto.getSiglaTipo()).isEqualTo("PL");
            assertThat(dto.getNumero()).isEqualTo("1234");
            assertThat(dto.getAno()).isEqualTo(2024);
            assertThat(dto.getIdOriginal()).isEqualTo("2342835");
        }

        @Test
        @DisplayName("monta canonical URL corretamente")
        void montaCanonicalUrl() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getCanonicalUrl())
                .isEqualTo("https://www.translegis.com.br/stat-proposicoes/camara-2342835/");
        }

        @Test
        @DisplayName("relacionadas sem página estática usam ficha oficial da Câmara")
        void relacionadasSemPaginaEstaticaUsamFichaOficial() {
            when(silverCamaraProposicaoRelacionadaRepository.findByProposicaoId("2342835"))
                .thenReturn(List.of(SilverCamaraProposicaoRelacionada.builder()
                    .proposicaoId("2342835")
                    .relacionadaId(2533419)
                    .relacionadaSiglaTipo("PRL")
                    .relacionadaNumero("1")
                    .relacionadaAno("0")
                    .relacionadaEmenta("Parecer do relator")
                    .build()));

            var dto = assembler.assemble(buildProposicaoCamara());

            assertThat(dto.getRelacionadas()).hasSize(1);
            assertThat(dto.getRelacionadas().get(0).getUrl())
                .isEqualTo("https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2533419");
        }

        @Test
        @DisplayName("relacionadas com página estática usam rota local")
        void relacionadasComPaginaEstaticaUsamRotaLocal() {
            when(silverCamaraProposicaoRelacionadaRepository.findByProposicaoId("2342835"))
                .thenReturn(List.of(SilverCamaraProposicaoRelacionada.builder()
                    .proposicaoId("2342835")
                    .relacionadaId(2533419)
                    .relacionadaSiglaTipo("PRL")
                    .relacionadaNumero("1")
                    .relacionadaAno("0")
                    .relacionadaEmenta("Parecer do relator")
                    .build()));
            when(proposicaoRepository.findByCasaAndIdOrigem(CasaLegislativa.CAMARA, "2533419"))
                .thenReturn(Optional.of(Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.CAMARA)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PRL")
                    .numero(1)
                    .ano(2024)
                    .idOrigem("2533419")
                    .build()));

            var dto = assembler.assemble(buildProposicaoCamara());

            assertThat(dto.getRelacionadas()).hasSize(1);
            assertThat(dto.getRelacionadas().get(0).getUrl()).isEqualTo("/stat-proposicoes/camara-2533419/");
        }

        @Test
        @DisplayName("monta SEO title com formato padrão")
        void montaSeoTitle() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getSeoTitle()).contains("PL 1234/2024");
            assertThat(dto.getSeoTitle()).contains("Câmara dos Deputados");
        }

        @Test
        @DisplayName("monta autores corretamente")
        void montaAutores() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getAutores()).hasSize(1);
            assertThat(dto.getAutores().get(0).getNome()).isEqualTo("João Silva");
            assertThat(dto.getAutores().get(0).getCasa()).isEqualTo("camara");
            assertThat(dto.getAutores().get(0).getIdOriginal()).isEqualTo("204560");
            assertThat(dto.getAutores().get(0).getPerfilUrl()).isEqualTo("/stat-parlamentares/camara-204560/");
            assertThat(dto.getAutores().get(0).isProponente()).isTrue();
        }

        @Test
        @DisplayName("monta autor com partido e UF")
        void montaAutorPartidoUf() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getAutores().get(0).getPartido()).isEqualTo("PT");
            assertThat(dto.getAutores().get(0).getUf()).isEqualTo("SP");
        }

        @Test
        @DisplayName("monta autoria resumida")
        void montaAutoriaResumo() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getAutoriaResumo()).isEqualTo("João Silva");
        }

        @Test
        @DisplayName("monta tramitações com data e órgão")
        void montaTramitacoes() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getTramitacoes()).hasSize(1);
            assertThat(dto.getTramitacoes().get(0).getDataFormatada()).isEqualTo("15/03/2024");
            assertThat(dto.getTramitacoes().get(0).getSiglaOrgao()).isEqualTo("MESA");
        }

        @Test
        @DisplayName("monta keywords como lista")
        void montaKeywords() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getKeywords()).containsExactlyInAnyOrder("transparência", "acesso à informação");
        }

        @Test
        @DisplayName("monta temas da Silver")
        void montaTemas() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getTemas()).containsExactly("Administração Pública");
        }

        @Test
        @DisplayName("monta JSON-LD de Legislation válido")
        void montaSchemaLegislation() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getSchemaOrgLegislationJson()).contains("\"@type\"");
            assertThat(dto.getSchemaOrgLegislationJson()).contains("Legislation");
            assertThat(dto.getSchemaOrgLegislationJson()).contains("PL 1234/2024");
        }

        @Test
        @DisplayName("monta JSON-LD de BreadcrumbList válido")
        void montaSchemaBreadcrumb() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getSchemaOrgBreadcrumbJson()).contains("BreadcrumbList");
            assertThat(dto.getSchemaOrgBreadcrumbJson()).contains("camara-2342835");
        }

        @Test
        @DisplayName("data de apresentação formatada em dd/MM/yyyy")
        void montaDataApresentacao() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getDataApresentacao()).isEqualTo("15/03/2024");
        }

        @Test
        @DisplayName("dataPublicacaoIso em formato ISO")
        void montaDataPublicacaoIso() {
            var dto = assembler.assemble(buildProposicaoCamara());
            assertThat(dto.getDataPublicacaoIso()).isEqualTo("2024-03-15");
        }

        @Nested
        @DisplayName("JSON embutido para hidratação Vue")
        class JsonEmbutido {

            private final ObjectMapper om = new ObjectMapper();

            @Test
            @DisplayName("proposicaoJsonEmbutido não é nulo após montagem")
            void naoNulo() {
                var dto = assembler.assemble(buildProposicaoCamara());
                assertThat(dto.getProposicaoJsonEmbutido()).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("JSON contém campos básicos compatíveis com ProposicaoCompleta")
            void contemCamposBasicos() throws Exception {
                var dto = assembler.assemble(buildProposicaoCamara());
                Map<String, Object> json = om.readValue(dto.getProposicaoJsonEmbutido(),
                        new TypeReference<>() {
                        });

                assertThat(json.get("id")).isEqualTo("camara:2342835");
                assertThat(json.get("idOriginal")).isEqualTo("2342835");
                assertThat(json.get("casa")).isEqualTo("camara");
                assertThat(json.get("siglaTipo")).isEqualTo("PL");
                assertThat(json.get("ano")).isEqualTo(2024);
                assertThat(json.get("situacaoTramitacao")).isEqualTo("tramitando");
                assertThat(json.get("dataApresentacao")).isEqualTo("2024-03-15");
                assertThat(json.get("autoria")).isEqualTo("João Silva");
            }

            @Test
            @DisplayName("tramitações no JSON têm dataHora em formato ISO")
            @SuppressWarnings("unchecked")
            void tramitacoesComDataHoraIso() throws Exception {
                var dto = assembler.assemble(buildProposicaoCamara());
                Map<String, Object> json = om.readValue(dto.getProposicaoJsonEmbutido(),
                        new TypeReference<>() {
                        });

                var tramitacoes = (List<Map<String, Object>>) json.get("tramitacoes");
                assertThat(tramitacoes).hasSize(1);
                // dataHora deve ser ISO (com 'T'), não dd/MM/yyyy
                assertThat((String) tramitacoes.get(0).get("dataHora")).contains("T");
                assertThat(tramitacoes.get(0).get("siglaOrgao")).isEqualTo("MESA");
            }

            @Test
            @DisplayName("autores no JSON têm campo proponente booleano")
            @SuppressWarnings("unchecked")
            void autoresComProponenteBooleano() throws Exception {
                var dto = assembler.assemble(buildProposicaoCamara());
                Map<String, Object> json = om.readValue(dto.getProposicaoJsonEmbutido(),
                        new TypeReference<>() {
                        });

                var autores = (List<Map<String, Object>>) json.get("autores");
                assertThat(autores).hasSize(1);
                assertThat(autores.get(0).get("nome")).isEqualTo("João Silva");
                assertThat(autores.get(0).get("proponente")).isEqualTo(true);
                assertThat(autores.get(0).get("casa")).isEqualTo("camara");
                assertThat(autores.get(0).get("idOriginal")).isEqualTo("204560");
            }
        }
    }

    // ── Senado ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Proposição do Senado")
    class Senado {

        private final UUID silverSenadoId = UUID.randomUUID();

        private Proposicao buildProposicaoSenado() {
            return buildProposicaoSenado("Em tramitação");
        }

        private Proposicao buildProposicaoSenado(String situacao) {
            return Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.SENADO)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PLS")
                    .numero(100)
                    .ano(2023)
                    .ementa("Altera a Lei de Diretrizes...")
                    .situacao(situacao)
                    .dataApresentacao(LocalDate.of(2023, 5, 10))
                    .idOrigem("12345")
                    .silverSenadoId(silverSenadoId)
                    .build();
        }

        @BeforeEach
        void mockSenadoData() {
            when(tramitacaoRepository.findByProposicaoIdOrdered(any())).thenReturn(List.of());
            when(silverSenadoAutoriaRepository.findBySenadoMateriaId(silverSenadoId))
                    .thenReturn(List.of(
                            SilverSenadoAutoria.builder()
                                    .nomeAutor("Maria Fontes")
                                    .descricaoTipoAutor("Senadora")
                                    .codigoParlamentar("1234")
                                    .siglaPartido("MDB")
                                    .ufParlamentar("RJ")
                                    .codigoTipoAutor("PARLAMENTAR")
                                    .build()));
                when(silverSenadoMateriaRepository.findById(Objects.requireNonNull(silverSenadoId)))
                    .thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("monta DTO com casa=senado")
        void montaCasaSenado() {
            var dto = assembler.assemble(buildProposicaoSenado());
            assertThat(dto.getCasa()).isEqualTo("senado");
            assertThat(dto.getCasaLabel()).isEqualTo("Senado Federal");
        }

        @Test
        @DisplayName("monta autores do Senado com link para senado")
        void montaAutoresSenado() {
            var dto = assembler.assemble(buildProposicaoSenado());
            assertThat(dto.getAutores()).hasSize(1);
            assertThat(dto.getAutores().get(0).getCasa()).isEqualTo("senado");
            assertThat(dto.getAutores().get(0).getNome()).isEqualTo("Maria Fontes");
            assertThat(dto.getAutores().get(0).getPerfilUrl()).isNull();
        }

        @Test
        @DisplayName("monta link de autor do Senado quando página estática do senador existe")
        void montaLinkAutorSenadoQuandoPaginaExiste() {
            when(silverSenadoSenadorRepository.findByCodigoSenador("1234"))
                    .thenReturn(Optional.of(SilverSenadoSenador.builder().codigoSenador("1234").build()));

            var dto = assembler.assemble(buildProposicaoSenado());

            assertThat(dto.getAutores()).hasSize(1);
            assertThat(dto.getAutores().get(0).getPerfilUrl()).isEqualTo("/stat-parlamentares/senado-1234/");
        }

        @Test
        @DisplayName("canonical URL usa senado")
        void montaCanonicalUrlSenado() {
            var dto = assembler.assemble(buildProposicaoSenado());
            assertThat(dto.getCanonicalUrl()).contains("/senado-12345");
        }

        @Test
        @DisplayName("normaliza situação legada do Senado no DTO e no JSON")
        void normalizaSituacaoLegadaSenado() throws Exception {
            var dto = assembler.assemble(buildProposicaoSenado("Sim"));

            assertThat(dto.getSituacaoDescricao()).isEqualTo("Em tramitação");
            assertThat(dto.getSituacaoTramitacao()).isEqualTo("tramitando");

            Map<String, Object> json = new ObjectMapper().readValue(dto.getProposicaoJsonEmbutido(),
                    new TypeReference<>() {
                    });
            assertThat(json.get("situacaoDescricao")).isEqualTo("Em tramitação");
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("sem silverCamaraId: autores e temas são listas vazias")
        void semSilverCamaraId_autoresETemasVazios() {
            var p = Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.CAMARA)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PL").numero(1).ano(2024).ementa("Ementa.")
                    .idOrigem("1")
                    .build();
            // silverCamaraId = null → não deve buscar no Silver
            when(tramitacaoRepository.findByProposicaoIdOrdered(any())).thenReturn(List.of());

            var dto = assembler.assemble(p);
            assertThat(dto.getAutores()).isEmpty();
            assertThat(dto.getTemas()).isEmpty();
        }

        @Test
        @DisplayName("geradoEm nunca é nulo")
        void geradoEmNuncaNulo() {
            var p = Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.CAMARA)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PL").numero(1).ano(2024).ementa("Ementa.")
                    .idOrigem("1")
                    .build();
            when(tramitacaoRepository.findByProposicaoIdOrdered(any())).thenReturn(List.of());

            var dto = assembler.assemble(p);
            assertThat(dto.getGeradoEm()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("keywords nulas resultam em lista vazia")
        void keywordsNulas_listaVazia() {
            var p = Proposicao.builder()
                    .id(UUID.randomUUID())
                    .casa(CasaLegislativa.CAMARA)
                    .tipo(TipoProposicao.LEI_ORDINARIA)
                    .sigla("PL").numero(1).ano(2024)
                    .ementa("Ementa.")
                    .idOrigem("1")
                    .keywords(null)
                    .build();
            when(tramitacaoRepository.findByProposicaoIdOrdered(any())).thenReturn(List.of());

            var dto = assembler.assemble(p);
            assertThat(dto.getKeywords()).isEmpty();
        }
    }
}
