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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoPresencaEvento;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoPresencaEventoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoPresencaEventoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoPresencaEventoLoader — insert-if-not-exists por (idDeputado, idEvento)")
class SilverCamaraDeputadoPresencaEventoLoaderTest {

    @Mock
    private SilverCamaraDeputadoPresencaEventoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoPresencaEventoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDeputadoPresencaEventoCSVRow novaRow(String idDeputado, String idEvento) {
        CamaraDeputadoPresencaEventoCSVRow row = new CamaraDeputadoPresencaEventoCSVRow();
        row.setIdDeputado(idDeputado);
        row.setIdEvento(idEvento);
        row.setDataHoraInicio("2023-06-01T09:00");
        row.setDataHoraFim("2023-06-01T12:00");
        row.setDescricao("Reunião ordinária");
        row.setDescricaoTipo("Reunião");
        row.setSituacao("REALIZADO");
        row.setUriEvento("https://camara.leg.br/eventos/" + idEvento);
        return row;
    }

    @Test
    @DisplayName("INSERT: presença nova é salva com todos os campos")
    void inserirPresencaNova() {
        CamaraDeputadoPresencaEventoCSVRow row = novaRow("111", "99001");
        when(repository.existsByIdDeputadoAndIdEvento("111", "99001")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoPresencaEvento> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoPresencaEvento.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoPresencaEvento salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getIdEvento()).isEqualTo("99001");
        assertThat(salvo.getSituacao()).isEqualTo("REALIZADO");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: presença já existente não é inserida")
    void ignorarPresencaExistente() {
        CamaraDeputadoPresencaEventoCSVRow row = novaRow("111", "99001");
        when(repository.existsByIdDeputadoAndIdEvento("111", "99001")).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraDeputadoPresencaEventoCSVRow row = novaRow("", "99001");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idEvento vazio é ignorada")
    void ignorarLinhaComIdEventoVazio() {
        CamaraDeputadoPresencaEventoCSVRow row = novaRow("111", "");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas presenças inseridas")
    void inserirMultiplasPresencas() {
        CamaraDeputadoPresencaEventoCSVRow row1 = novaRow("111", "99001");
        CamaraDeputadoPresencaEventoCSVRow row2 = novaRow("222", "99002");
        when(repository.existsByIdDeputadoAndIdEvento(any(), any())).thenReturn(false);
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
