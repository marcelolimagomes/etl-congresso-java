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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOrgao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOrgaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoOrgaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoOrgaoLoader — insert-if-not-exists por (idDeputado, idOrgao, dataInicio)")
class SilverCamaraDeputadoOrgaoLoaderTest {

    @Mock
    private SilverCamaraDeputadoOrgaoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoOrgaoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDeputadoOrgaoCSVRow novaRow(String idDeputado, String idOrgao, String dataInicio) {
        CamaraDeputadoOrgaoCSVRow row = new CamaraDeputadoOrgaoCSVRow();
        row.setIdDeputado(idDeputado);
        row.setIdOrgao(idOrgao);
        row.setSiglaOrgao("CAEX");
        row.setNomeOrgao("Comissão de Assuntos Externos");
        row.setNomePublicacao("Comissão Exterior");
        row.setTitulo("Membro");
        row.setCodTitulo("M");
        row.setDataInicio(dataInicio);
        row.setDataFim("2023-12-31");
        row.setUriOrgao("https://camara.leg.br/orgaos/123");
        return row;
    }

    @Test
    @DisplayName("INSERT: órgão novo é salvo com todos os campos")
    void inserirOrgaoNovo() {
        CamaraDeputadoOrgaoCSVRow row = novaRow("111", "200", "2023-01-01");
        when(repository.existsByIdDeputadoAndIdOrgaoAndDataInicio("111", "200", "2023-01-01"))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoOrgao> captor = ArgumentCaptor.forClass(SilverCamaraDeputadoOrgao.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoOrgao salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getIdOrgao()).isEqualTo("200");
        assertThat(salvo.getDataInicio()).isEqualTo("2023-01-01");
        assertThat(salvo.getSiglaOrgao()).isEqualTo("CAEX");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: participação em órgão já existente não é inserida")
    void ignorarOrgaoExistente() {
        CamaraDeputadoOrgaoCSVRow row = novaRow("111", "200", "2023-01-01");
        when(repository.existsByIdDeputadoAndIdOrgaoAndDataInicio("111", "200", "2023-01-01"))
                .thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraDeputadoOrgaoCSVRow row = novaRow("", "200", "2023-01-01");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idOrgao vazio é ignorada")
    void ignorarLinhaComIdOrgaoVazio() {
        CamaraDeputadoOrgaoCSVRow row = novaRow("111", "", "2023-01-01");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos órgãos novos são inseridos")
    void inserirMultiplosOrgaos() {
        CamaraDeputadoOrgaoCSVRow row1 = novaRow("111", "200", "2023-01-01");
        CamaraDeputadoOrgaoCSVRow row2 = novaRow("111", "201", "2023-01-01");
        when(repository.existsByIdDeputadoAndIdOrgaoAndDataInicio(any(), any(), any()))
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
