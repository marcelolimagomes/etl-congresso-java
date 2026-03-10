package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoRelacionada;
import br.leg.congresso.etl.extractor.camara.dto.CamaraRelacionadaDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRelacionadaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraRelacionadasLoader — upsert por (proposicaoId, relacionadaId)")
class SilverCamaraRelacionadasLoaderTest {

    @Mock
    private SilverCamaraProposicaoRelacionadaRepository repository;

    @InjectMocks
    private SilverCamaraRelacionadasLoader loader;

    private UUID jobId;
    private SilverCamaraProposicao proposicao;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
        proposicao = SilverCamaraProposicao.builder()
                .id(UUID.randomUUID())
                .camaraId("123456")
                .etlJobId(jobId)
                .build();
    }

    private CamaraRelacionadaDTO dtoRelacionada(long id, String siglaTipo, String ementa) {
        CamaraRelacionadaDTO dto = new CamaraRelacionadaDTO();
        dto.setId(id);
        dto.setUri("https://camara.leg.br/proposicoes/" + id);
        dto.setSiglaTipo(siglaTipo);
        dto.setNumero("100");
        dto.setAno("2024");
        dto.setEmenta(ementa);
        dto.setCodTipo(1);
        return dto;
    }

    @Test
    @DisplayName("retorna 0 para proposicao nula")
    void retornaZeroParaProposicaoNula() {
        assertThat(loader.carregar(null, List.of(dtoRelacionada(1L, "PL", "ementa")), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista nula")
    void retornaZeroParaListaNula() {
        assertThat(loader.carregar(proposicao, null, jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista vazia")
    void retornaZeroParaListaVazia() {
        assertThat(loader.carregar(proposicao, Collections.emptyList(), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 se proposicao sem camaraId")
    void retornaZeroSemCamaraId() {
        SilverCamaraProposicao semId = SilverCamaraProposicao.builder().id(UUID.randomUUID()).build();
        assertThat(loader.carregar(semId, List.of(dtoRelacionada(1L, "PL", "ementa")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTOs com id null")
    void ignoraDtoComIdNull() {
        CamaraRelacionadaDTO dto = new CamaraRelacionadaDTO();
        dto.setId(null);

        assertThat(loader.carregar(proposicao, List.of(dto), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere nova relacionada quando não existe")
    void insereNovaRelacionada() {
        when(repository.existsByProposicaoIdAndRelacionadaId(anyString(), anyInt())).thenReturn(false);

        CamaraRelacionadaDTO dto = dtoRelacionada(9999L, "PL", "Altera disposições sobre educação");
        int resultado = loader.carregar(proposicao, List.of(dto), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraProposicaoRelacionada> captor = ArgumentCaptor
                .forClass(SilverCamaraProposicaoRelacionada.class);
        verify(repository).save(captor.capture());

        SilverCamaraProposicaoRelacionada salvo = captor.getValue();
        assertThat(salvo.getProposicaoId()).isEqualTo("123456");
        assertThat(salvo.getRelacionadaId()).isEqualTo(9999);
        assertThat(salvo.getRelacionadaSiglaTipo()).isEqualTo("PL");
        assertThat(salvo.getRelacionadaEmenta()).isEqualTo("Altera disposições sobre educação");
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ignora relacionada já existente (idempotente)")
    void ignoraRelacionadaJaExistente() {
        when(repository.existsByProposicaoIdAndRelacionadaId(anyString(), anyInt())).thenReturn(true);

        assertThat(loader.carregar(proposicao, List.of(dtoRelacionada(1L, "PL", "ementa")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplas relacionadas novas")
    void insereMultiplasRelacionadas() {
        when(repository.existsByProposicaoIdAndRelacionadaId(anyString(), anyInt())).thenReturn(false);

        List<CamaraRelacionadaDTO> dtos = List.of(
                dtoRelacionada(1L, "PL", "Ementa 1"),
                dtoRelacionada(2L, "PEC", "Ementa 2"),
                dtoRelacionada(3L, "MPV", "Ementa 3"));

        assertThat(loader.carregar(proposicao, dtos, jobId)).isEqualTo(3);
        verify(repository, times(3)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        when(repository.existsByProposicaoIdAndRelacionadaId("123456", 10)).thenReturn(true);
        when(repository.existsByProposicaoIdAndRelacionadaId("123456", 20)).thenReturn(false);

        List<CamaraRelacionadaDTO> dtos = List.of(
                dtoRelacionada(10L, "PL", "Existente"),
                dtoRelacionada(20L, "PEC", "Nova"));

        assertThat(loader.carregar(proposicao, dtos, jobId)).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
