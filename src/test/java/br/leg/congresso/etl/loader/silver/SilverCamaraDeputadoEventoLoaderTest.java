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

import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoEvento;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoEventoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoEventoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoEventoLoader — insert-if-not-exists por (camaraDeputadoId, idEvento)")
class SilverCamaraDeputadoEventoLoaderTest {

    @Mock
    private SilverCamaraDeputadoEventoRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SilverCamaraDeputadoEventoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String DEP_ID = "204554";

    private CamaraDeputadoEventoDTO novoDto(String idEvento) {
        CamaraDeputadoEventoDTO dto = new CamaraDeputadoEventoDTO();
        dto.setId(idEvento);
        dto.setDataHoraInicio("2024-03-10T10:00");
        dto.setDataHoraFim("2024-03-10T12:00");
        dto.setDescricao("Sessão Ordinária");
        dto.setDescricaoTipo("Sessão");
        dto.setSituacao("Encerrada");
        CamaraDeputadoEventoDTO.LocalCamara local = new CamaraDeputadoEventoDTO.LocalCamara();
        local.setNome("Plenário");
        local.setPredio("Plenário");
        local.setSala("Principal");
        local.setAndar("Térreo");
        dto.setLocalCamara(local);
        return dto;
    }

    @Test
    @DisplayName("INSERT: evento novo é salvo com localCamara planificado")
    void inserirEventoNovo() {
        CamaraDeputadoEventoDTO dto = novoDto("12345");
        when(repository.existsByCamaraDeputadoIdAndIdEvento(DEP_ID, "12345")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoEvento> captor = ArgumentCaptor.forClass(SilverCamaraDeputadoEvento.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoEvento salvo = captor.getValue();
        assertThat(salvo.getCamaraDeputadoId()).isEqualTo(DEP_ID);
        assertThat(salvo.getIdEvento()).isEqualTo("12345");
        assertThat(salvo.getLocalCamaraNome()).isEqualTo("Plenário");
        assertThat(salvo.getLocalCamaraSala()).isEqualTo("Principal");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("SKIP: evento já existente não é inserido novamente")
    void ignorarEventoExistente() {
        CamaraDeputadoEventoDTO dto = novoDto("12345");
        when(repository.existsByCamaraDeputadoIdAndIdEvento(DEP_ID, "12345")).thenReturn(true);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com id nulo é ignorado")
    void ignorarDtoComIdNulo() {
        CamaraDeputadoEventoDTO dto = new CamaraDeputadoEventoDTO();
        dto.setId(null);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com id vazio é ignorado")
    void ignorarDtoComIdVazio() {
        CamaraDeputadoEventoDTO dto = new CamaraDeputadoEventoDTO();
        dto.setId("");

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos eventos novos são inseridos")
    void inserirMultiplosEventos() {
        CamaraDeputadoEventoDTO dto1 = novoDto("11111");
        CamaraDeputadoEventoDTO dto2 = novoDto("22222");
        when(repository.existsByCamaraDeputadoIdAndIdEvento(any(), any())).thenReturn(false);
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
