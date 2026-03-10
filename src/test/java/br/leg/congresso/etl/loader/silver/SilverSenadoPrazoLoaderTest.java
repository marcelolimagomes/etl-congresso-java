package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoPrazo;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPrazoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoPrazoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoPrazoLoader — upsert por (senado_materia_id, tipo_prazo, data_inicio)")
class SilverSenadoPrazoLoaderTest {

    @Mock
    private SilverSenadoPrazoRepository repository;

    @InjectMocks
    private SilverSenadoPrazoLoader loader;

    private UUID jobId;
    private SilverSenadoMateria materia;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
        materia = SilverSenadoMateria.builder()
                .id(UUID.randomUUID())
                .codigo("162431")
                .etlJobId(jobId)
                .build();
    }

    private SenadoPrazoDTO dto(String tipoPrazo, String dataInicio) {
        SenadoPrazoDTO dto = new SenadoPrazoDTO();
        dto.setTipoPrazo(tipoPrazo);
        dto.setDataInicio(dataInicio);
        dto.setDataFim("2024-03-30");
        dto.setDescricao("Descrição do prazo");
        dto.setColegiado("CCJC");
        dto.setSituacao("ABERTO");
        return dto;
    }

    @Test
    @DisplayName("retorna 0 para materia nula")
    void retornaZeroParaMateriaNula() {
        assertThat(loader.carregar(null, List.of(dto("EMENDA", "2024-01-01")), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista nula")
    void retornaZeroParaListaNula() {
        assertThat(loader.carregar(materia, null, jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista vazia")
    void retornaZeroParaListaVazia() {
        assertThat(loader.carregar(materia, Collections.emptyList(), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("ignora DTO com tipoPrazo nulo")
    void ignoraDtoComTipoPrazoNulo() {
        SenadoPrazoDTO dtoSemTipo = new SenadoPrazoDTO();
        dtoSemTipo.setTipoPrazo(null);
        dtoSemTipo.setDataInicio("2024-01-01");

        assertThat(loader.carregar(materia, List.of(dtoSemTipo), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTO com dataInicio nula")
    void ignoraDtoComDataInicioNula() {
        SenadoPrazoDTO dtoSemData = new SenadoPrazoDTO();
        dtoSemData.setTipoPrazo("EMENDA");
        dtoSemData.setDataInicio(null);

        assertThat(loader.carregar(materia, List.of(dtoSemData), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTO com dataInicio em branco")
    void ignoraDtoComDataInicioEmBranco() {
        SenadoPrazoDTO dtoDataEmBranco = dto("EMENDA", "  ");

        assertThat(loader.carregar(materia, List.of(dtoDataEmBranco), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere novo prazo quando não existe")
    void insereNovoPrazo() {
        when(repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(any(), anyString(), anyString()))
                .thenReturn(false);

        int resultado = loader.carregar(materia, List.of(dto("EMENDA", "2024-01-10")), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoPrazo> captor = ArgumentCaptor.forClass(SilverSenadoPrazo.class);
        verify(repository).save(captor.capture());

        SilverSenadoPrazo salvo = captor.getValue();
        assertThat(salvo.getTipoPrazo()).isEqualTo("EMENDA");
        assertThat(salvo.getDataInicio()).isEqualTo("2024-01-10");
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        assertThat(salvo.getCodigoMateria()).isEqualTo("162431");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ignora prazo já existente (idempotente)")
    void ignoraPrazoJaExistente() {
        when(repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(any(), anyString(), anyString()))
                .thenReturn(true);

        assertThat(loader.carregar(materia, List.of(dto("EMENDA", "2024-01-10")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplos prazos novos")
    void insereMultiplosPrazos() {
        when(repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(any(), anyString(), anyString()))
                .thenReturn(false);

        List<SenadoPrazoDTO> dtos = List.of(
                dto("EMENDA", "2024-01-10"),
                dto("ADITAMENTO", "2024-02-05"));

        assertThat(loader.carregar(materia, dtos, jobId)).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        when(repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(
                materia.getId(), "EMENDA", "2024-01-10")).thenReturn(true);
        when(repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(
                materia.getId(), "ADITAMENTO", "2024-02-05")).thenReturn(false);

        assertThat(loader.carregar(materia,
                List.of(dto("EMENDA", "2024-01-10"), dto("ADITAMENTO", "2024-02-05")), jobId))
                .isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
