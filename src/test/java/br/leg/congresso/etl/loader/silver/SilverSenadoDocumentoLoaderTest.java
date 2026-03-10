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

import br.leg.congresso.etl.domain.silver.SilverSenadoDocumento;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDocumentoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoDocumentoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoDocumentoLoader — upsert por (senado_materia_id, codigo_documento)")
class SilverSenadoDocumentoLoaderTest {

    @Mock
    private SilverSenadoDocumentoRepository repository;

    @InjectMocks
    private SilverSenadoDocumentoLoader loader;

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

    private SenadoDocumentoDTO dto(String codigoDocumento, String tipoDocumento) {
        SenadoDocumentoDTO dto = new SenadoDocumentoDTO();
        dto.setCodigoDocumento(codigoDocumento);
        dto.setTipoDocumento(tipoDocumento);
        dto.setDescricaoTipoDocumento("Desc " + tipoDocumento);
        dto.setDataDocumento("2024-03-10");
        dto.setDescricaoDocumento("Texto do documento");
        dto.setUrlDocumento("https://legis.senado.leg.br/doc/" + codigoDocumento);
        dto.setAutorNome("Senador Teste");
        return dto;
    }

    @Test
    @DisplayName("retorna 0 para materia nula")
    void retornaZeroParaMateriaNula() {
        assertThat(loader.carregar(null, List.of(dto("D-001", "TEX")), jobId)).isZero();
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
    @DisplayName("ignora DTO com codigoDocumento nulo")
    void ignoraDtoComCodigoNulo() {
        SenadoDocumentoDTO dtoSemCodigo = new SenadoDocumentoDTO();
        dtoSemCodigo.setCodigoDocumento(null);

        assertThat(loader.carregar(materia, List.of(dtoSemCodigo), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTO com codigoDocumento em branco")
    void ignoraDtoComCodigoEmBranco() {
        SenadoDocumentoDTO dtoEmBranco = new SenadoDocumentoDTO();
        dtoEmBranco.setCodigoDocumento("  ");

        assertThat(loader.carregar(materia, List.of(dtoEmBranco), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere novo documento quando não existe")
    void insereNovoDocumento() {
        when(repository.existsBySenadoMateriaIdAndCodigoDocumento(any(), anyString())).thenReturn(false);

        int resultado = loader.carregar(materia, List.of(dto("D-001", "TEX")), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoDocumento> captor = ArgumentCaptor.forClass(SilverSenadoDocumento.class);
        verify(repository).save(captor.capture());

        SilverSenadoDocumento salvo = captor.getValue();
        assertThat(salvo.getCodigoDocumento()).isEqualTo("D-001");
        assertThat(salvo.getTipoDocumento()).isEqualTo("TEX");
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        assertThat(salvo.getCodigoMateria()).isEqualTo("162431");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ignora documento já existente (idempotente)")
    void ignoraDocumentoJaExistente() {
        when(repository.existsBySenadoMateriaIdAndCodigoDocumento(any(), anyString())).thenReturn(true);

        assertThat(loader.carregar(materia, List.of(dto("D-001", "TEX")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplos documentos novos")
    void insereMultiplosDocumentos() {
        when(repository.existsBySenadoMateriaIdAndCodigoDocumento(any(), anyString())).thenReturn(false);

        assertThat(loader.carregar(materia, List.of(dto("D-001", "TEX"), dto("D-002", "EMD")), jobId))
                .isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        when(repository.existsBySenadoMateriaIdAndCodigoDocumento(materia.getId(), "D-001")).thenReturn(true);
        when(repository.existsBySenadoMateriaIdAndCodigoDocumento(materia.getId(), "D-002")).thenReturn(false);

        assertThat(loader.carregar(materia, List.of(dto("D-001", "TEX"), dto("D-002", "EMD")), jobId))
                .isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
