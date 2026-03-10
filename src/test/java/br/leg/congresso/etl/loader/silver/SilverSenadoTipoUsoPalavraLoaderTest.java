package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverSenadoTipoUsoPalavra;
import br.leg.congresso.etl.extractor.senado.dto.SenadoTipoUsoPalavraDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoTipoUsoPalavraRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoTipoUsoPalavraLoader — insert-if-not-exists por codigo_tipo")
class SilverSenadoTipoUsoPalavraLoaderTest {

    @Mock
    private SilverSenadoTipoUsoPalavraRepository repository;

    @InjectMocks
    private SilverSenadoTipoUsoPalavraLoader loader;

    private final UUID jobId = UUID.randomUUID();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SenadoTipoUsoPalavraDTO.TipoUsoPalavra tipo(
            String codigo, String descricao, String sigla) {

        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t = new SenadoTipoUsoPalavraDTO.TipoUsoPalavra();
        t.setCodigo(codigo);
        t.setDescricao(descricao);
        t.setSigla(sigla);
        t.setIndicadorAtivo("Sim");
        return t;
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: tipo novo é salvo com todos os campos")
    void inserirTipoNovo() {
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t = tipo("1", "Discurso", "DS");
        when(repository.existsByCodigoTipo("1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(t), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoTipoUsoPalavra> captor = ArgumentCaptor.forClass(SilverSenadoTipoUsoPalavra.class);
        verify(repository).save(captor.capture());

        SilverSenadoTipoUsoPalavra salvo = captor.getValue();
        assertThat(salvo.getCodigoTipo()).isEqualTo("1");
        assertThat(salvo.getDescricaoTipo()).isEqualTo("Discurso");
        assertThat(salvo.getAbreviatura()).isEqualTo("DS");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("SKIP: tipo já existente não é inserido novamente")
    void ignorarTipoExistente() {
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t = tipo("1", "Discurso", "DS");
        when(repository.existsByCodigoTipo("1")).thenReturn(true);

        int resultado = loader.carregar(List.of(t), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: tipo com codigo nulo é ignorado")
    void ignorarTipoComCodigoNulo() {
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t = tipo(null, "Discurso", "DS");

        int resultado = loader.carregar(List.of(t), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: tipo com codigo vazio é ignorado")
    void ignorarTipoComCodigoVazio() {
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t = tipo("  ", "Discurso", "DS");

        int resultado = loader.carregar(List.of(t), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos tipos novos são inseridos")
    void inserirMultiplosTipos() {
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t1 = tipo("1", "Discurso", "DS");
        SenadoTipoUsoPalavraDTO.TipoUsoPalavra t2 = tipo("2", "Aparte", "AP");
        when(repository.existsByCodigoTipo(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(t1, t2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero sem chamar o repositório")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("NULL: lista nula retorna zero sem lançar exceção")
    void listaNulaRetornaZero() {
        int resultado = loader.carregar(null, jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
