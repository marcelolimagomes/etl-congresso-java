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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoVotacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoVotacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoVotacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoVotacaoLoader — upsert por (senado_materia_id, codigo_sessao_votacao, sequencial_sessao)")
class SilverSenadoVotacaoLoaderTest {

    @Mock
    private SilverSenadoVotacaoRepository repository;

    // ObjectMapper real para não precisar mockar serialização JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SilverSenadoVotacaoLoader loader;

    private UUID jobId;
    private SilverSenadoMateria materia;

    @BeforeEach
    void setup() {
        loader = new SilverSenadoVotacaoLoader(repository, objectMapper);
        jobId = UUID.randomUUID();
        materia = SilverSenadoMateria.builder()
                .id(UUID.randomUUID())
                .codigo("162431")
                .etlJobId(jobId)
                .build();
    }

    private SenadoVotacaoDTO dto(String codigoSessaoVotacao, String sequencialSessao) {
        SenadoVotacaoDTO dto = new SenadoVotacaoDTO();
        dto.setCodigoSessao("SESS-001");
        dto.setSiglaCasa("SF");
        dto.setCodigoSessaoVotacao(codigoSessaoVotacao);
        dto.setSequencialSessao(sequencialSessao);
        dto.setDataSessao("2024-03-15");
        dto.setDescricaoVotacao("Votação em plenário");
        dto.setResultado("APROVADO");
        dto.setDescricaoResultado("Aprovado por maioria");
        dto.setTotalVotosSim(45);
        dto.setTotalVotosNao(12);
        dto.setTotalVotosAbstencao(3);
        dto.setIndicadorVotacaoSecreta("N");
        return dto;
    }

    @Test
    @DisplayName("retorna 0 para materia nula")
    void retornaZeroParaMateriaNula() {
        assertThat(loader.carregar(null, List.of(dto("VOT-001", "1")), jobId)).isZero();
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
    @DisplayName("ignora DTO com codigoSessaoVotacao nulo")
    void ignoraDtoComCodigoSessaoVotacaoNulo() {
        SenadoVotacaoDTO dtoSemCodigo = new SenadoVotacaoDTO();
        dtoSemCodigo.setCodigoSessaoVotacao(null);

        assertThat(loader.carregar(materia, List.of(dtoSemCodigo), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignora DTO com codigoSessaoVotacao em branco")
    void ignoraDtoComCodigoSessaoVotacaoEmBranco() {
        SenadoVotacaoDTO dtoEmBranco = new SenadoVotacaoDTO();
        dtoEmBranco.setCodigoSessaoVotacao("  ");

        assertThat(loader.carregar(materia, List.of(dtoEmBranco), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere nova votação quando não existe")
    void insereNovaVotacao() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                any(), anyString(), any()))
                .thenReturn(false);

        int resultado = loader.carregar(materia, List.of(dto("VOT-001", "1")), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoVotacao> captor = ArgumentCaptor.forClass(SilverSenadoVotacao.class);
        verify(repository).save(captor.capture());

        SilverSenadoVotacao salvo = captor.getValue();
        assertThat(salvo.getCodigoSessaoVotacao()).isEqualTo("VOT-001");
        assertThat(salvo.getSequencialSessao()).isEqualTo("1");
        assertThat(salvo.getResultado()).isEqualTo("APROVADO");
        assertThat(salvo.getTotalVotosSim()).isEqualTo(45);
        assertThat(salvo.getTotalVotosNao()).isEqualTo(12);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        assertThat(salvo.getCodigoMateria()).isEqualTo("162431");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ignora votação já existente (idempotente)")
    void ignoraVotacaoJaExistente() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                any(), anyString(), any()))
                .thenReturn(true);

        assertThat(loader.carregar(materia, List.of(dto("VOT-001", "1")), jobId)).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("insere múltiplas votações novas")
    void insereMultiplasVotacoes() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                any(), anyString(), any()))
                .thenReturn(false);

        List<SenadoVotacaoDTO> dtos = List.of(
                dto("VOT-001", "1"),
                dto("VOT-002", "2"));

        assertThat(loader.carregar(materia, dtos, jobId)).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                materia.getId(), "VOT-001", "1"))
                .thenReturn(true);
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                materia.getId(), "VOT-002", "2"))
                .thenReturn(false);

        assertThat(loader.carregar(materia,
                List.of(dto("VOT-001", "1"), dto("VOT-002", "2")), jobId))
                .isEqualTo(1);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("serializa votos parlamentares como JSONB quando preenchidos")
    void serializaVotosParlamentares() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                any(), anyString(), any()))
                .thenReturn(false);

        SenadoVotacaoDTO dtoComVotos = dto("VOT-003", "3");
        dtoComVotos.setVotosParlamentares(List.of("voto1", "voto2"));

        loader.carregar(materia, List.of(dtoComVotos), jobId);

        ArgumentCaptor<SilverSenadoVotacao> captor = ArgumentCaptor.forClass(SilverSenadoVotacao.class);
        verify(repository).save(captor.capture());

        SilverSenadoVotacao salvo = captor.getValue();
        assertThat(salvo.getVotosParlamentares()).isNotNull();
        assertThat(salvo.getVotosParlamentares()).contains("voto1");
    }

    @Test
    @DisplayName("armazena null em votos parlamentares quando lista vazia")
    void armazenaNullParaVotosVazios() {
        when(repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                any(), anyString(), any()))
                .thenReturn(false);

        SenadoVotacaoDTO dtoSemVotos = dto("VOT-004", "4");
        dtoSemVotos.setVotosParlamentares(null);

        loader.carregar(materia, List.of(dtoSemVotos), jobId);

        ArgumentCaptor<SilverSenadoVotacao> captor = ArgumentCaptor.forClass(SilverSenadoVotacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVotosParlamentares()).isNull();
    }
}
