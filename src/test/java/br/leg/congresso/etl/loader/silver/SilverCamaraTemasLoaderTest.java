package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoTemaCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoTemaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraTemasLoader — upsert por (uriProposicao, codTema)")
class SilverCamaraTemasLoaderTest {

    @Mock
    private SilverCamaraProposicaoTemaRepository repository;

    @InjectMocks
    private SilverCamaraTemasLoader loader;

    private UUID jobId;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
    }

    private CamaraProposicaoTemaCSVRow temaRow(String uri, String codTema, String tema) {
        CamaraProposicaoTemaCSVRow row = new CamaraProposicaoTemaCSVRow();
        row.setUriProposicao(uri);
        row.setCodTema(codTema);
        row.setTema(tema);
        row.setSiglaTipo("PL");
        row.setNumero("100");
        row.setAno("2024");
        row.setRelevancia("1");
        return row;
    }

    @Test
    @DisplayName("retorna 0 para lista nula")
    void retornaZeroParaListaNula() {
        assertThat(loader.carregar(null, jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista vazia")
    void retornaZeroParaListaVazia() {
        assertThat(loader.carregar(Collections.emptyList(), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("ignora linhas com uriProposicao nulo")
    void ignoraLinhasComUriNulo() {
        CamaraProposicaoTemaCSVRow row = new CamaraProposicaoTemaCSVRow();
        row.setUriProposicao(null);
        row.setCodTema("10");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora linhas com codTema nulo ou não numérico")
    void ignoraLinhasComCodTemaNaoNumerico() {
        CamaraProposicaoTemaCSVRow row = new CamaraProposicaoTemaCSVRow();
        row.setUriProposicao("https://camara.leg.br/proposicoes/123");
        row.setCodTema("NaN");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere novo tema quando não existe")
    void insereNovoTema() {
        when(repository.existsByUriProposicaoAndCodTema(anyString(), anyInt())).thenReturn(false);

        CamaraProposicaoTemaCSVRow row = temaRow("https://camara.leg.br/proposicoes/123", "42", "Educação");
        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraProposicaoTema> captor = ArgumentCaptor.forClass(SilverCamaraProposicaoTema.class);
        verify(repository).save(captor.capture());

        SilverCamaraProposicaoTema salvo = captor.getValue();
        assertThat(salvo.getUriProposicao()).isEqualTo("https://camara.leg.br/proposicoes/123");
        assertThat(salvo.getCodTema()).isEqualTo(42);
        assertThat(salvo.getTema()).isEqualTo("Educação");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("ignora tema já existente (idempotente)")
    void ignoraTemaJaExistente() {
        when(repository.existsByUriProposicaoAndCodTema(anyString(), anyInt())).thenReturn(true);

        CamaraProposicaoTemaCSVRow row = temaRow("https://camara.leg.br/proposicoes/123", "42", "Educação");
        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplos temas novos de uma vez")
    void insereMultiplosTemas() {
        when(repository.existsByUriProposicaoAndCodTema(anyString(), anyInt())).thenReturn(false);

        List<CamaraProposicaoTemaCSVRow> rows = List.of(
                temaRow("https://camara.leg.br/proposicoes/1", "10", "Economia"),
                temaRow("https://camara.leg.br/proposicoes/1", "20", "Saúde"),
                temaRow("https://camara.leg.br/proposicoes/2", "10", "Economia"));

        int resultado = loader.carregar(rows, jobId);
        assertThat(resultado).isEqualTo(3);
        verify(repository, times(3)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura de novos e existentes")
    void insereSomenteNovos() {
        when(repository.existsByUriProposicaoAndCodTema("https://camara.leg.br/1", 10)).thenReturn(true);
        when(repository.existsByUriProposicaoAndCodTema("https://camara.leg.br/2", 20)).thenReturn(false);

        List<CamaraProposicaoTemaCSVRow> rows = List.of(
                temaRow("https://camara.leg.br/1", "10", "Economia"),
                temaRow("https://camara.leg.br/2", "20", "Saúde"));

        int resultado = loader.carregar(rows, jobId);
        assertThat(resultado).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("mapeia relevancia como Integer corretamente")
    void mapeiaRelevancia() {
        when(repository.existsByUriProposicaoAndCodTema(anyString(), anyInt())).thenReturn(false);

        CamaraProposicaoTemaCSVRow row = temaRow("https://camara.leg.br/proposicoes/1", "5", "Meio Ambiente");
        row.setRelevancia("3");

        loader.carregar(List.of(row), jobId);

        ArgumentCaptor<SilverCamaraProposicaoTema> captor = ArgumentCaptor.forClass(SilverCamaraProposicaoTema.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRelevancia()).isEqualTo(3);
    }
}
