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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoFrente;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoFrenteCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoFrenteRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoFrenteLoader — insert-if-not-exists por (idDeputado, idFrente)")
class SilverCamaraDeputadoFrenteLoaderTest {

    @Mock
    private SilverCamaraDeputadoFrenteRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoFrenteLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDeputadoFrenteCSVRow novaRow(String idDeputado, String idFrente) {
        CamaraDeputadoFrenteCSVRow row = new CamaraDeputadoFrenteCSVRow();
        row.setIdDeputado(idDeputado);
        row.setIdFrente(idFrente);
        row.setIdLegislatura("57");
        row.setTitulo("Frente Parlamentar teste " + idFrente);
        row.setUri("https://camara.leg.br/frentes/" + idFrente);
        return row;
    }

    @Test
    @DisplayName("INSERT: frente nova é salva com todos os campos")
    void inserirFrenteNova() {
        CamaraDeputadoFrenteCSVRow row = novaRow("111", "54321");
        when(repository.existsByIdDeputadoAndIdFrente("111", "54321")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoFrente> captor = ArgumentCaptor.forClass(SilverCamaraDeputadoFrente.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoFrente salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getIdFrente()).isEqualTo("54321");
        assertThat(salvo.getIdLegislatura()).isEqualTo("57");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: frente já existente não é inserida novamente")
    void ignorarFrenteExistente() {
        CamaraDeputadoFrenteCSVRow row = novaRow("111", "54321");
        when(repository.existsByIdDeputadoAndIdFrente("111", "54321")).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraDeputadoFrenteCSVRow row = novaRow("", "54321");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idFrente vazio é ignorada")
    void ignorarLinhaComIdFrenteVazio() {
        CamaraDeputadoFrenteCSVRow row = novaRow("111", "");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas frentes novas são inseridas")
    void inserirMultiplasFrentes() {
        CamaraDeputadoFrenteCSVRow row1 = novaRow("111", "54321");
        CamaraDeputadoFrenteCSVRow row2 = novaRow("222", "54322");
        when(repository.existsByIdDeputadoAndIdFrente(any(), any())).thenReturn(false);
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
