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

import br.leg.congresso.etl.domain.silver.SilverSenadoEmenda;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoEmendaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoEmendaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoEmendaLoader — upsert por (senado_materia_id, codigo_emenda)")
class SilverSenadoEmendaLoaderTest {

    @Mock
    private SilverSenadoEmendaRepository repository;

    @InjectMocks
    private SilverSenadoEmendaLoader loader;

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

    private SenadoEmendaDTO dto(String codigoEmenda, String tipoEmenda) {
        SenadoEmendaDTO dto = new SenadoEmendaDTO();
        dto.setCodigoEmenda(codigoEmenda);
        dto.setTipoEmenda(tipoEmenda);
        dto.setDescricaoTipoEmenda("Desc " + tipoEmenda);
        dto.setNumeroEmenda("1");
        dto.setDataApresentacao("2024-01-15");
        dto.setAutorNome("Senador Teste");
        dto.setEmenta("Ementa da emenda");
        return dto;
    }

    @Test
    @DisplayName("retorna 0 para materia nula")
    void retornaZeroParaMateriaNula() {
        assertThat(loader.carregar(null, List.of(dto("123", "SUB")), jobId)).isZero();
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
    @DisplayName("ignora DTO com codigoEmenda nulo")
    void ignoraDtoComCodigoNulo() {
        SenadoEmendaDTO dtoSemCodigo = new SenadoEmendaDTO();
        dtoSemCodigo.setCodigoEmenda(null);

        assertThat(loader.carregar(materia, List.of(dtoSemCodigo), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTO com codigoEmenda em branco")
    void ignoraDtoComCodigoEmBranco() {
        SenadoEmendaDTO dtoEmBranco = new SenadoEmendaDTO();
        dtoEmBranco.setCodigoEmenda("   ");

        assertThat(loader.carregar(materia, List.of(dtoEmBranco), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere nova emenda quando não existe")
    void insereNovaEmenda() {
        when(repository.existsBySenadoMateriaIdAndCodigoEmenda(any(), anyString())).thenReturn(false);

        int resultado = loader.carregar(materia, List.of(dto("E-001", "SUB")), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoEmenda> captor = ArgumentCaptor.forClass(SilverSenadoEmenda.class);
        verify(repository).save(captor.capture());

        SilverSenadoEmenda salvo = captor.getValue();
        assertThat(salvo.getCodigoEmenda()).isEqualTo("E-001");
        assertThat(salvo.getTipoEmenda()).isEqualTo("SUB");
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        assertThat(salvo.getCodigoMateria()).isEqualTo("162431");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ignora emenda já existente (idempotente)")
    void ignoraEmendaJaExistente() {
        when(repository.existsBySenadoMateriaIdAndCodigoEmenda(any(), anyString())).thenReturn(true);

        assertThat(loader.carregar(materia, List.of(dto("E-001", "SUB")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplas emendas novas")
    void insereMultiplasEmendas() {
        when(repository.existsBySenadoMateriaIdAndCodigoEmenda(any(), anyString())).thenReturn(false);

        assertThat(loader.carregar(materia, List.of(dto("E-001", "SUB"), dto("E-002", "RED")), jobId))
                .isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        when(repository.existsBySenadoMateriaIdAndCodigoEmenda(materia.getId(), "E-001")).thenReturn(true);
        when(repository.existsBySenadoMateriaIdAndCodigoEmenda(materia.getId(), "E-002")).thenReturn(false);

        assertThat(loader.carregar(materia, List.of(dto("E-001", "SUB"), dto("E-002", "RED")), jobId))
                .isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
