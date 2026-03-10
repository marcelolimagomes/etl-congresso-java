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

import br.leg.congresso.etl.domain.silver.SilverCamaraGrupoMembro;
import br.leg.congresso.etl.extractor.camara.dto.CamaraGrupoMembroCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraGrupoMembroRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraGrupoMembroLoader — insert-if-not-exists por (idDeputado, idGrupo)")
class SilverCamaraGrupoMembroLoaderTest {

    @Mock
    private SilverCamaraGrupoMembroRepository repository;

    @InjectMocks
    private SilverCamaraGrupoMembroLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraGrupoMembroCSVRow novaRow(String idDeputado, String idGrupo) {
        CamaraGrupoMembroCSVRow row = new CamaraGrupoMembroCSVRow();
        row.setIdDeputado(idDeputado);
        row.setIdGrupo(idGrupo);
        row.setNomeParlamentar("Dep. Teste " + idDeputado);
        row.setUri("https://camara.leg.br/deputados/" + idDeputado);
        row.setTitulo("Membro");
        row.setDataInicio("2023-01-01");
        row.setDataFim("2023-12-31");
        return row;
    }

    @Test
    @DisplayName("INSERT: membro de grupo novo é salvo com todos os campos")
    void inserirMembroNovo() {
        CamaraGrupoMembroCSVRow row = novaRow("111", "G001");
        when(repository.existsByIdDeputadoAndIdGrupoAndDataInicio("111", "G001", "2023-01-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraGrupoMembro> captor = ArgumentCaptor.forClass(SilverCamaraGrupoMembro.class);
        verify(repository).save(captor.capture());
        SilverCamaraGrupoMembro salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getIdGrupo()).isEqualTo("G001");
        assertThat(salvo.getNomeParlamentar()).isEqualTo("Dep. Teste 111");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: membro já existente no grupo não é inserido")
    void ignorarMembroExistente() {
        CamaraGrupoMembroCSVRow row = novaRow("111", "G001");
        when(repository.existsByIdDeputadoAndIdGrupoAndDataInicio("111", "G001", "2023-01-01")).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraGrupoMembroCSVRow row = novaRow("", "G001");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idGrupo vazio é ignorada")
    void ignorarLinhaComIdGrupoVazio() {
        CamaraGrupoMembroCSVRow row = novaRow("111", "");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos membros novos são inseridos")
    void inserirMultiplosMembros() {
        CamaraGrupoMembroCSVRow row1 = novaRow("111", "G001");
        CamaraGrupoMembroCSVRow row2 = novaRow("222", "G001");
        when(repository.existsByIdDeputadoAndIdGrupoAndDataInicio(any(), any(), any())).thenReturn(false);
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
