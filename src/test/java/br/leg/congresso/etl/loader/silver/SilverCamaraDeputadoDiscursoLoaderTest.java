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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoDiscurso;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoDiscursoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoDiscursoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoDiscursoLoader — insert-if-not-exists por (camaraDeputadoId, dataHoraInicio, tipoDiscurso)")
class SilverCamaraDeputadoDiscursoLoaderTest {

    @Mock
    private SilverCamaraDeputadoDiscursoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoDiscursoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String DEP_ID = "204554";

    private CamaraDeputadoDiscursoDTO novoDto(String dataHoraInicio, String tipoDiscurso) {
        CamaraDeputadoDiscursoDTO dto = new CamaraDeputadoDiscursoDTO();
        dto.setDataHoraInicio(dataHoraInicio);
        dto.setDataHoraFim("2024-03-10T16:00");
        dto.setTipoDiscurso(tipoDiscurso);
        dto.setSumario("Sumário do discurso");
        dto.setTranscricao("Texto completo");
        dto.setKeywords("saude;educacao");
        dto.setUrlTexto("https://camara.leg.br/discurso/texto");
        CamaraDeputadoDiscursoDTO.FaseEvento fase = new CamaraDeputadoDiscursoDTO.FaseEvento();
        fase.setTitulo("Ordem do Dia");
        fase.setDataHoraInicio("2024-03-10T14:00");
        fase.setDataHoraFim("2024-03-10T18:00");
        dto.setFaseEvento(fase);
        return dto;
    }

    @Test
    @DisplayName("INSERT: discurso novo é salvo com todos os campos, incluindo faseEvento planificado")
    void inserirDiscursoNovo() {
        CamaraDeputadoDiscursoDTO dto = novoDto("2024-03-10T14:30", "PEQUENO_EXPEDIENTE");
        when(repository.existsByCamaraDeputadoIdAndDataHoraInicioAndTipoDiscurso(
                DEP_ID, "2024-03-10T14:30", "PEQUENO_EXPEDIENTE")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoDiscurso> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoDiscurso.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoDiscurso salvo = captor.getValue();
        assertThat(salvo.getCamaraDeputadoId()).isEqualTo(DEP_ID);
        assertThat(salvo.getDataHoraInicio()).isEqualTo("2024-03-10T14:30");
        assertThat(salvo.getTipoDiscurso()).isEqualTo("PEQUENO_EXPEDIENTE");
        assertThat(salvo.getFaseEventoTitulo()).isEqualTo("Ordem do Dia");
        assertThat(salvo.getFaseEventoDataHoraInicio()).isEqualTo("2024-03-10T14:00");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("SKIP: discurso já existente não é inserido novamente")
    void ignorarDiscursoExistente() {
        CamaraDeputadoDiscursoDTO dto = novoDto("2024-03-10T14:30", "PEQUENO_EXPEDIENTE");
        when(repository.existsByCamaraDeputadoIdAndDataHoraInicioAndTipoDiscurso(
                DEP_ID, "2024-03-10T14:30", "PEQUENO_EXPEDIENTE")).thenReturn(true);

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com dataHoraInicio nulo é ignorado")
    void ignorarDtoComDataHoraInicioNulo() {
        CamaraDeputadoDiscursoDTO dto = new CamaraDeputadoDiscursoDTO();
        dto.setDataHoraInicio(null);
        dto.setTipoDiscurso("DISCURSO");

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: DTO com dataHoraInicio vazio é ignorado")
    void ignorarDtoComDataHoraInicioVazio() {
        CamaraDeputadoDiscursoDTO dto = new CamaraDeputadoDiscursoDTO();
        dto.setDataHoraInicio("");
        dto.setTipoDiscurso("DISCURSO");

        int resultado = loader.carregar(DEP_ID, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos discursos novos são inseridos")
    void inserirMultiplosDiscursos() {
        CamaraDeputadoDiscursoDTO dto1 = novoDto("2024-03-10T14:30", "PEQUENO_EXPEDIENTE");
        CamaraDeputadoDiscursoDTO dto2 = novoDto("2024-03-11T10:00", "ORDEM_DO_DIA");
        when(repository.existsByCamaraDeputadoIdAndDataHoraInicioAndTipoDiscurso(any(), any(), any()))
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
