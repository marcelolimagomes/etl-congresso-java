package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoAutorCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraAutoresLoader — upsert por (uriProposicao, nomeAutor, ordemAssinatura)")
class SilverCamaraAutoresLoaderTest {

    @Mock
    private SilverCamaraProposicaoAutorRepository repository;

    @InjectMocks
    private SilverCamaraAutoresLoader loader;

    private UUID jobId;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
    }

    private CamaraProposicaoAutorCSVRow autorRow(String uri, String nomeAutor, String ordemAssinatura) {
        CamaraProposicaoAutorCSVRow row = new CamaraProposicaoAutorCSVRow();
        row.setUriProposicao(uri);
        row.setNomeAutor(nomeAutor);
        row.setOrdemAssinatura(ordemAssinatura);
        row.setIdProposicao("12345");
        row.setIdDeputadoAutor("678");
        row.setUriAutor("https://camara.leg.br/deputados/678");
        row.setCodTipoAutor("1");
        row.setTipoAutor("Deputado");
        row.setSiglaPartidoAutor("PT");
        row.setUriPartidoAutor("https://camara.leg.br/partidos/10");
        row.setSiglaUfAutor("SP");
        row.setProponente("1");
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
        CamaraProposicaoAutorCSVRow row = autorRow(null, "João Silva", "1");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora linhas com uriProposicao em branco")
    void ignoraLinhasComUriEmBranco() {
        CamaraProposicaoAutorCSVRow row = autorRow("   ", "João Silva", "1");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora linhas com nomeAutor nulo")
    void ignoraLinhasComNomeAutorNulo() {
        CamaraProposicaoAutorCSVRow row = autorRow("https://camara.leg.br/proposicoes/123", null, "1");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora linhas com nomeAutor em branco")
    void ignoraLinhasComNomeAutorEmBranco() {
        CamaraProposicaoAutorCSVRow row = autorRow("https://camara.leg.br/proposicoes/123", "  ", "1");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere novo autor quando não existe")
    void insereNovoAutor() {
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(anyString(), anyString(), anyInt()))
                .thenReturn(false);

        CamaraProposicaoAutorCSVRow row = autorRow(
                "https://camara.leg.br/proposicoes/123", "Maria Souza", "2");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraProposicaoAutor> captor = ArgumentCaptor.forClass(SilverCamaraProposicaoAutor.class);
        verify(repository).save(captor.capture());

        SilverCamaraProposicaoAutor salvo = captor.getValue();
        assertThat(salvo.getUriProposicao()).isEqualTo("https://camara.leg.br/proposicoes/123");
        assertThat(salvo.getNomeAutor()).isEqualTo("Maria Souza");
        assertThat(salvo.getOrdemAssinatura()).isEqualTo(2);
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
        assertThat(salvo.getSiglaPartidoAutor()).isEqualTo("PT");
        assertThat(salvo.getSiglaUfAutor()).isEqualTo("SP");
    }

    @Test
    @DisplayName("ignora autor já existente (idempotente)")
    void ignoraAutorJaExistente() {
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(anyString(), anyString(), anyInt()))
                .thenReturn(true);

        CamaraProposicaoAutorCSVRow row = autorRow(
                "https://camara.leg.br/proposicoes/123", "Maria Souza", "2");

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplos autores novos de uma vez")
    void insereMultiplosAutores() {
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(anyString(), anyString(), anyInt()))
                .thenReturn(false);

        List<CamaraProposicaoAutorCSVRow> rows = List.of(
                autorRow("https://camara.leg.br/proposicoes/1", "Ana Lima", "1"),
                autorRow("https://camara.leg.br/proposicoes/1", "Carlos Melo", "2"),
                autorRow("https://camara.leg.br/proposicoes/2", "Ana Lima", "1"));

        int resultado = loader.carregar(rows, jobId);
        assertThat(resultado).isEqualTo(3);
        verify(repository, times(3)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura de novos e existentes")
    void insereSomenteNovos() {
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(
                "https://camara.leg.br/1", "Ana Lima", 1)).thenReturn(true);
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(
                "https://camara.leg.br/2", "Carlos Melo", 1)).thenReturn(false);

        List<CamaraProposicaoAutorCSVRow> rows = List.of(
                autorRow("https://camara.leg.br/1", "Ana Lima", "1"),
                autorRow("https://camara.leg.br/2", "Carlos Melo", "1"));

        int resultado = loader.carregar(rows, jobId);
        assertThat(resultado).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("trata ordemAssinatura nulo como null")
    void trataOrdemAssinaturaNula() {
        when(repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(anyString(), anyString(), isNull()))
                .thenReturn(false);

        CamaraProposicaoAutorCSVRow row = autorRow("https://camara.leg.br/proposicoes/1", "Pedro Costa", null);

        int resultado = loader.carregar(List.of(row), jobId);
        assertThat(resultado).isEqualTo(1);

        ArgumentCaptor<SilverCamaraProposicaoAutor> captor = ArgumentCaptor.forClass(SilverCamaraProposicaoAutor.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrdemAssinatura()).isNull();
    }
}
