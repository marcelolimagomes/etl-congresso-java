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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoHistorico;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoHistoricoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoHistoricoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoHistoricoLoader — insert-if-not-exists por (camaraDeputadoId, dataHora, idLegislatura)")
class SilverCamaraDeputadoHistoricoLoaderTest {

    @Mock
    private SilverCamaraDeputadoHistoricoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoHistoricoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String DEP_ID = "204554";

    private CamaraDeputadoHistoricoDTO novoDto(String dataHora, Integer idLegislatura) {
        CamaraDeputadoHistoricoDTO dto = new CamaraDeputadoHistoricoDTO();
        dto.setId("7890");
        dto.setDataHora(dataHora);
        dto.setIdLegislatura(idLegislatura);
        dto.setNome("Deputado Teste");
        dto.setNomeEleitoral("Dep. Teste");
        dto.setSiglaPartido("PT");
        dto.setSiglaUf("SP");
        dto.setSituacao("Exercício");
        dto.setCondicaoEleitoral("Titular");
        dto.setDescricaoStatus("Status descrito");
        dto.setEmail("dep@camara.leg.br");
        return dto;
    }

    @Test
    @DisplayName("INSERT: histórico novo é salvo com idLegislatura convertido para String")
    void inserirHistoricoNovo() {
        CamaraDeputadoHistoricoDTO dto = novoDto("2024-01-01T00:00", 57);
        when(repository.existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(
                DEP_ID, "2024-01-01T00:00", "57")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoHistorico> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoHistorico.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoHistorico salvo = captor.getValue();
        assertThat(salvo.getCamaraDeputadoId()).isEqualTo(DEP_ID);
        assertThat(salvo.getDataHora()).isEqualTo("2024-01-01T00:00");
        assertThat(salvo.getIdLegislatura()).isEqualTo("57");
        assertThat(salvo.getCamaraIdRegistro()).isEqualTo("7890");
        assertThat(salvo.getNome()).isEqualTo("Deputado Teste");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("SKIP: histórico já existente não é inserido novamente")
    void ignorarHistoricoExistente() {
        CamaraDeputadoHistoricoDTO dto = novoDto("2024-01-01T00:00", 57);
        when(repository.existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(
                DEP_ID, "2024-01-01T00:00", "57")).thenReturn(true);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com dataHora nulo é ignorado")
    void ignorarDtoComDataHoraNulo() {
        CamaraDeputadoHistoricoDTO dto = novoDto(null, 57);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com dataHora vazio é ignorado")
    void ignorarDtoComDataHoraVazio() {
        CamaraDeputadoHistoricoDTO dto = novoDto("", 57);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("INSERT: idLegislatura nulo é tratado como null String")
    void idLegislaturaComNullEhTratado() {
        CamaraDeputadoHistoricoDTO dto = novoDto("2024-01-01T00:00", null);
        when(repository.existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(
                DEP_ID, "2024-01-01T00:00", null)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoHistorico> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoHistorico.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIdLegislatura()).isNull();
    }

    @Test
    @DisplayName("BATCH: múltiplos históricos novos são inseridos")
    void inserirMultiplosHistoricos() {
        CamaraDeputadoHistoricoDTO dto1 = novoDto("2024-01-01T00:00", 57);
        CamaraDeputadoHistoricoDTO dto2 = novoDto("2020-02-01T00:00", 56);
        when(repository.existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(any(), any(), any()))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto1, dto2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero sem chamar o repositório")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(DEP_ID, Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
