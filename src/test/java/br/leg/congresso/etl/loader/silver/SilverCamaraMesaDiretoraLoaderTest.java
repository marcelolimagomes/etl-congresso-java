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

import br.leg.congresso.etl.domain.silver.SilverCamaraMesaDiretora;
import br.leg.congresso.etl.extractor.camara.dto.CamaraMesaDiretoraCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraMesaDiretoraRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraMesaDiretoraLoader — insert-if-not-exists por (idDeputado, titulo, idLegislatura)")
class SilverCamaraMesaDiretoraLoaderTest {

    @Mock
    private SilverCamaraMesaDiretoraRepository repository;

    @InjectMocks
    private SilverCamaraMesaDiretoraLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraMesaDiretoraCSVRow novaRow(String idDeputado, String titulo, String idLeg) {
        CamaraMesaDiretoraCSVRow row = new CamaraMesaDiretoraCSVRow();
        row.setIdDeputado(idDeputado);
        row.setTitulo(titulo);
        row.setIdLegislatura(idLeg);
        row.setDataInicio("2023-02-01");
        row.setDataFim("2025-01-31");
        return row;
    }

    @Test
    @DisplayName("INSERT: membro da Mesa novo é salvo com todos os campos")
    void inserirMembroNovo() {
        CamaraMesaDiretoraCSVRow row = novaRow("111", "Presidente", "57");
        when(repository.existsByIdDeputadoAndIdLegislaturaAndTituloAndDataInicio("111", "57", "Presidente",
                "2023-02-01"))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraMesaDiretora> captor = ArgumentCaptor.forClass(SilverCamaraMesaDiretora.class);
        verify(repository).save(captor.capture());
        SilverCamaraMesaDiretora salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getTitulo()).isEqualTo("Presidente");
        assertThat(salvo.getIdLegislatura()).isEqualTo("57");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: membro já existente não é inserido")
    void ignorarMembroExistente() {
        CamaraMesaDiretoraCSVRow row = novaRow("111", "Presidente", "57");
        when(repository.existsByIdDeputadoAndIdLegislaturaAndTituloAndDataInicio("111", "57", "Presidente",
                "2023-02-01"))
                .thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraMesaDiretoraCSVRow row = novaRow("", "Presidente", "57");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com titulo vazio é ignorada")
    void ignorarLinhaComTituloVazio() {
        CamaraMesaDiretoraCSVRow row = novaRow("111", "", "57");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idLegislatura vazio é ignorada")
    void ignorarLinhaComIdLegislaturaVazio() {
        CamaraMesaDiretoraCSVRow row = novaRow("111", "Presidente", "");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos membros novos são inseridos")
    void inserirMultiplosMembros() {
        CamaraMesaDiretoraCSVRow row1 = novaRow("111", "Presidente", "57");
        CamaraMesaDiretoraCSVRow row2 = novaRow("222", "Vice-Presidente", "57");
        when(repository.existsByIdDeputadoAndIdLegislaturaAndTituloAndDataInicio(any(), any(), any(), any()))
                .thenReturn(false);
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
