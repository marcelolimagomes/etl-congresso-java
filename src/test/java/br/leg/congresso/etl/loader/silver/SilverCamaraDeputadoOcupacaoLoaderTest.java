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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOcupacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOcupacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoOcupacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoOcupacaoLoader — insert-if-not-exists por (idDeputado, titulo, anoInicio, entidade)")
class SilverCamaraDeputadoOcupacaoLoaderTest {

    @Mock
    private SilverCamaraDeputadoOcupacaoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoOcupacaoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDeputadoOcupacaoCSVRow novaRow(String idDeputado, String titulo) {
        CamaraDeputadoOcupacaoCSVRow row = new CamaraDeputadoOcupacaoCSVRow();
        row.setIdDeputado(idDeputado);
        row.setTitulo(titulo);
        row.setAnoInicio("2010");
        row.setAnoFim("2014");
        row.setEntidade("OAB");
        row.setEntidadeUF("SP");
        row.setEntidadePais("Brasil");
        return row;
    }

    @Test
    @DisplayName("INSERT: ocupação nova é salva com todos os campos")
    void inserirOcupacaoNova() {
        CamaraDeputadoOcupacaoCSVRow row = novaRow("111", "Advogado");
        when(repository.existsByIdDeputadoAndTituloAndAnoInicio(
                "111", "Advogado", "2010")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoOcupacao> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoOcupacao.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoOcupacao salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getTitulo()).isEqualTo("Advogado");
        assertThat(salvo.getAnoInicio()).isEqualTo("2010");
        assertThat(salvo.getEntidade()).isEqualTo("OAB");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: ocupação já existente não é inserida novamente")
    void ignorarOcupacaoExistente() {
        CamaraDeputadoOcupacaoCSVRow row = novaRow("111", "Advogado");
        when(repository.existsByIdDeputadoAndTituloAndAnoInicio(
                "111", "Advogado", "2010")).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraDeputadoOcupacaoCSVRow row = novaRow("", "Advogado");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas ocupações novas são inseridas")
    void inserirMultiplasOcupacoes() {
        CamaraDeputadoOcupacaoCSVRow row1 = novaRow("111", "Advogado");
        CamaraDeputadoOcupacaoCSVRow row2 = novaRow("222", "Médico");
        when(repository.existsByIdDeputadoAndTituloAndAnoInicio(
                any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row1, row2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
