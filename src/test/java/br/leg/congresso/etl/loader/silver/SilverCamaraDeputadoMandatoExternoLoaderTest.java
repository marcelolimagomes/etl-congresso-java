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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoMandatoExterno;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoMandatoExternoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoMandatoExternoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoMandatoExternoLoader — insert-if-not-exists por (camaraDeputadoId, cargo, siglaUf, anoInicio)")
class SilverCamaraDeputadoMandatoExternoLoaderTest {

    @Mock
    private SilverCamaraDeputadoMandatoExternoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoMandatoExternoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String DEP_ID = "204554";

    private CamaraDeputadoMandatoExternoDTO novoDto(String cargo, String siglaUf, String anoInicio) {
        CamaraDeputadoMandatoExternoDTO dto = new CamaraDeputadoMandatoExternoDTO();
        dto.setCargo(cargo);
        dto.setSiglaUf(siglaUf);
        dto.setAnoInicio(anoInicio);
        dto.setAnoFim("2020");
        dto.setMunicipio("São Paulo");
        dto.setSiglaPartidoEleicao("PT");
        dto.setUriPartidoEleicao("https://camara.leg.br/partido/PT");
        return dto;
    }

    @Test
    @DisplayName("INSERT: mandato externo novo é salvo com todos os campos")
    void inserirMandatoNovo() {
        CamaraDeputadoMandatoExternoDTO dto = novoDto("Vereador", "SP", "2016");
        when(repository.existsByCamaraDeputadoIdAndCargoAndSiglaUfAndAnoInicio(
                DEP_ID, "Vereador", "SP", "2016")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoMandatoExterno> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoMandatoExterno.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoMandatoExterno salvo = captor.getValue();
        assertThat(salvo.getCamaraDeputadoId()).isEqualTo(DEP_ID);
        assertThat(salvo.getCargo()).isEqualTo("Vereador");
        assertThat(salvo.getSiglaUf()).isEqualTo("SP");
        assertThat(salvo.getAnoInicio()).isEqualTo("2016");
        assertThat(salvo.getMunicipio()).isEqualTo("São Paulo");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("SKIP: mandato já existente não é inserido novamente")
    void ignorarMandatoExistente() {
        CamaraDeputadoMandatoExternoDTO dto = novoDto("Vereador", "SP", "2016");
        when(repository.existsByCamaraDeputadoIdAndCargoAndSiglaUfAndAnoInicio(
                DEP_ID, "Vereador", "SP", "2016")).thenReturn(true);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com cargo nulo é ignorado")
    void ignorarDtoComCargoNulo() {
        CamaraDeputadoMandatoExternoDTO dto = new CamaraDeputadoMandatoExternoDTO();
        dto.setCargo(null);
        dto.setSiglaUf("SP");
        dto.setAnoInicio("2016");

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com cargo vazio é ignorado")
    void ignorarDtoComCargoVazio() {
        CamaraDeputadoMandatoExternoDTO dto = new CamaraDeputadoMandatoExternoDTO();
        dto.setCargo("");
        dto.setSiglaUf("SP");
        dto.setAnoInicio("2016");

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos mandatos novos são inseridos")
    void inserirMultiplosMandatos() {
        CamaraDeputadoMandatoExternoDTO dto1 = novoDto("Vereador", "SP", "2016");
        CamaraDeputadoMandatoExternoDTO dto2 = novoDto("Deputado Estadual", "SP", "2014");
        when(repository.existsByCamaraDeputadoIdAndCargoAndSiglaUfAndAnoInicio(any(), any(), any(), any()))
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

    @Test
    @DisplayName("NULL: lista nula retorna zero sem lançar exceção")
    void listaNulaRetornaZero() {
        int resultado = loader.carregar(DEP_ID, null, jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
