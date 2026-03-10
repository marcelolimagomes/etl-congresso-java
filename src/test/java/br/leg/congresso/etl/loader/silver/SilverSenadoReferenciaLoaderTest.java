package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverSenadoRefAssunto;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefClasse;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefSigla;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoAutor;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoDecisao;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoSituacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRefDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefAssuntoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefClasseRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefSiglaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoDecisaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoSituacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoReferenciaLoader — full-replace por tipo de referência")
class SilverSenadoReferenciaLoaderTest {

    @Mock
    private SilverSenadoRefTipoSituacaoRepository tipoSituacaoRepository;
    @Mock
    private SilverSenadoRefTipoDecisaoRepository tipoDecisaoRepository;
    @Mock
    private SilverSenadoRefTipoAutorRepository tipoAutorRepository;
    @Mock
    private SilverSenadoRefSiglaRepository siglaRepository;
    @Mock
    private SilverSenadoRefClasseRepository classeRepository;
    @Mock
    private SilverSenadoRefAssuntoRepository assuntoRepository;

    private SilverSenadoReferenciaLoader loader;
    private UUID jobId;

    @BeforeEach
    void setup() {
        loader = new SilverSenadoReferenciaLoader(
                tipoSituacaoRepository,
                tipoDecisaoRepository,
                tipoAutorRepository,
                siglaRepository,
                classeRepository,
                assuntoRepository);
        jobId = UUID.randomUUID();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private SenadoRefDTO dtoCodigoDescricao(String codigo, String descricao) {
        SenadoRefDTO dto = new SenadoRefDTO();
        dto.setCodigo(codigo);
        dto.setDescricao(descricao);
        return dto;
    }

    private SenadoRefDTO dtoSigla(String sigla, String descricao, String classe) {
        SenadoRefDTO dto = new SenadoRefDTO();
        dto.setSigla(sigla);
        dto.setDescricao(descricao);
        dto.setClasse(classe);
        return dto;
    }

    private SenadoRefDTO dtoClasse(String codigo, String descricao, String classePai) {
        SenadoRefDTO dto = new SenadoRefDTO();
        dto.setCodigo(codigo);
        dto.setDescricao(descricao);
        dto.setClassePai(classePai);
        return dto;
    }

    private SenadoRefDTO dtoAssunto(String codigo, String geral, String especifico) {
        SenadoRefDTO dto = new SenadoRefDTO();
        dto.setCodigo(codigo);
        dto.setAssuntoGeral(geral);
        dto.setAssuntoEspecifico(especifico);
        return dto;
    }

    // ─── TiposSituacao ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarTiposSituacao")
    class TiposSituacao {

        @Test
        @DisplayName("retorna 0 para lista nula — sem interação com repositório")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarTiposSituacao(null, jobId)).isZero();
            verifyNoInteractions(tipoSituacaoRepository);
        }

        @Test
        @DisplayName("retorna 0 para lista vazia — sem interação com repositório")
        void retornaZeroParaListaVazia() {
            assertThat(loader.carregarTiposSituacao(Collections.emptyList(), jobId)).isZero();
            verifyNoInteractions(tipoSituacaoRepository);
        }

        @Test
        @DisplayName("filtra DTOs com codigo nulo ou vazio")
        void filtrarDtosComCodigoInvalido() {
            SenadoRefDTO semCodigo = new SenadoRefDTO();
            SenadoRefDTO codBranco = dtoCodigoDescricao("  ", "Descricao");

            int result = loader.carregarTiposSituacao(List.of(semCodigo, codBranco), jobId);

            assertThat(result).isZero();
            verify(tipoSituacaoRepository).deleteAll();
            verify(tipoSituacaoRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("executa full-replace e retorna quantidade carregada")
        void executaFullReplaceComSucesso() {
            List<SenadoRefDTO> dtos = List.of(
                    dtoCodigoDescricao("SIT01", "Situação 1"),
                    dtoCodigoDescricao("SIT02", "Situação 2"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefTipoSituacao>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarTiposSituacao(dtos, jobId);

            assertThat(result).isEqualTo(2);
            verify(tipoSituacaoRepository).deleteAll();
            verify(tipoSituacaoRepository).saveAll(captor.capture());

            List<SilverSenadoRefTipoSituacao> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getCodigo()).isEqualTo("SIT01");
            assertThat(saved.get(0).getDescricao()).isEqualTo("Situação 1");
            assertThat(saved.get(0).getEtlJobId()).isEqualTo(jobId);
            assertThat(saved.get(1).getCodigo()).isEqualTo("SIT02");
        }
    }

    // ─── TiposDecisao ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarTiposDecisao")
    class TiposDecisao {

        @Test
        @DisplayName("retorna 0 para lista nula")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarTiposDecisao(null, jobId)).isZero();
            verifyNoInteractions(tipoDecisaoRepository);
        }

        @Test
        @DisplayName("retorna 0 para lista vazia")
        void retornaZeroParaListaVazia() {
            assertThat(loader.carregarTiposDecisao(Collections.emptyList(), jobId)).isZero();
            verifyNoInteractions(tipoDecisaoRepository);
        }

        @Test
        @DisplayName("executa full-replace e retorna quantidade carregada")
        void executaFullReplaceComSucesso() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefTipoDecisao>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarTiposDecisao(
                    List.of(dtoCodigoDescricao("DEC01", "Decisão aprovada")), jobId);

            assertThat(result).isEqualTo(1);
            verify(tipoDecisaoRepository).deleteAll();
            verify(tipoDecisaoRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getCodigo()).isEqualTo("DEC01");
        }
    }

    // ─── TiposAutor ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarTiposAutor")
    class TiposAutor {

        @Test
        @DisplayName("retorna 0 para lista nula")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarTiposAutor(null, jobId)).isZero();
            verifyNoInteractions(tipoAutorRepository);
        }

        @Test
        @DisplayName("retorna 0 para lista vazia")
        void retornaZeroParaListaVazia() {
            assertThat(loader.carregarTiposAutor(Collections.emptyList(), jobId)).isZero();
            verifyNoInteractions(tipoAutorRepository);
        }

        @Test
        @DisplayName("executa full-replace e retorna quantidade carregada")
        void executaFullReplaceComSucesso() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefTipoAutor>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarTiposAutor(
                    List.of(dtoCodigoDescricao("AUT01", "Senador")), jobId);

            assertThat(result).isEqualTo(1);
            verify(tipoAutorRepository).deleteAll();
            verify(tipoAutorRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getCodigo()).isEqualTo("AUT01");
        }
    }

    // ─── Siglas ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarSiglas")
    class Siglas {

        @Test
        @DisplayName("retorna 0 para lista nula")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarSiglas(null, jobId)).isZero();
            verifyNoInteractions(siglaRepository);
        }

        @Test
        @DisplayName("filtra DTOs com sigla nula ou vazia")
        void filtrarDtosComSiglaNula() {
            SenadoRefDTO semSigla = new SenadoRefDTO();

            int result = loader.carregarSiglas(List.of(semSigla), jobId);

            assertThat(result).isZero();
            verify(siglaRepository).deleteAll();
            verify(siglaRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("executa full-replace mapeando todos os campos")
        void executaFullReplaceComTodosCampos() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefSigla>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarSiglas(
                    List.of(dtoSigla("PL", "Projeto de Lei", "Projetos")), jobId);

            assertThat(result).isEqualTo(1);
            verify(siglaRepository).deleteAll();
            verify(siglaRepository).saveAll(captor.capture());

            SilverSenadoRefSigla saved = captor.getValue().get(0);
            assertThat(saved.getSigla()).isEqualTo("PL");
            assertThat(saved.getDescricao()).isEqualTo("Projeto de Lei");
            assertThat(saved.getClasse()).isEqualTo("Projetos");
            assertThat(saved.getEtlJobId()).isEqualTo(jobId);
        }
    }

    // ─── Classes ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarClasses")
    class Classes {

        @Test
        @DisplayName("retorna 0 para lista nula")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarClasses(null, jobId)).isZero();
            verifyNoInteractions(classeRepository);
        }

        @Test
        @DisplayName("executa full-replace mapeando classePai")
        void executaFullReplaceComClassePai() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefClasse>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarClasses(
                    List.of(dtoClasse("CLS01", "Classe Teste", "CLS-PAI")), jobId);

            assertThat(result).isEqualTo(1);
            verify(classeRepository).deleteAll();
            verify(classeRepository).saveAll(captor.capture());

            SilverSenadoRefClasse saved = captor.getValue().get(0);
            assertThat(saved.getCodigo()).isEqualTo("CLS01");
            assertThat(saved.getClassePai()).isEqualTo("CLS-PAI");
        }
    }

    // ─── Assuntos ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("carregarAssuntos")
    class Assuntos {

        @Test
        @DisplayName("retorna 0 para lista nula")
        void retornaZeroParaListaNula() {
            assertThat(loader.carregarAssuntos(null, jobId)).isZero();
            verifyNoInteractions(assuntoRepository);
        }

        @Test
        @DisplayName("retorna 0 para lista vazia")
        void retornaZeroParaListaVazia() {
            assertThat(loader.carregarAssuntos(Collections.emptyList(), jobId)).isZero();
            verifyNoInteractions(assuntoRepository);
        }

        @Test
        @DisplayName("executa full-replace mapeando assuntoGeral e assuntoEspecifico")
        void executaFullReplaceMapeandoCampos() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SilverSenadoRefAssunto>> captor = ArgumentCaptor.forClass(List.class);

            int result = loader.carregarAssuntos(
                    List.of(dtoAssunto("ASS01", "Educação", "Educação Básica")), jobId);

            assertThat(result).isEqualTo(1);
            verify(assuntoRepository).deleteAll();
            verify(assuntoRepository).saveAll(captor.capture());

            SilverSenadoRefAssunto saved = captor.getValue().get(0);
            assertThat(saved.getCodigo()).isEqualTo("ASS01");
            assertThat(saved.getAssuntoGeral()).isEqualTo("Educação");
            assertThat(saved.getAssuntoEspecifico()).isEqualTo("Educação Básica");
            assertThat(saved.getEtlJobId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("filtra DTOs com codigo nulo")
        void filtrarDtosComCodigoNulo() {
            SenadoRefDTO semCodigo = dtoAssunto(null, "Geral", "Especifico");

            int result = loader.carregarAssuntos(List.of(semCodigo), jobId);

            assertThat(result).isZero();
            verify(assuntoRepository).deleteAll();
            verify(assuntoRepository).saveAll(List.of());
        }
    }
}
