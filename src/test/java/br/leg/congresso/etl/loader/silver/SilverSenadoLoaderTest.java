package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.transformer.silver.SilverSenadoHashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoLoader — upsert por codigo com hash de conteúdo")
class SilverSenadoLoaderTest {

    @Mock
    private SilverSenadoMateriaRepository repository;

    @Mock
    private SilverSenadoHashGenerator hashGenerator;

    @Mock
    private EtlMetrics etlMetrics;

    @InjectMocks
    private SilverSenadoLoader loader;

    private EtlJobControl jobControl;

    @BeforeEach
    void setup() {
        jobControl = EtlJobControl.builder()
            .id(UUID.randomUUID())
            .origem(CasaLegislativa.SENADO)
            .tipoExecucao(TipoExecucao.FULL)
            .iniciadoEm(LocalDateTime.now())
            .build();
    }

    private SilverSenadoMateria novaMateria(String codigo) {
        return SilverSenadoMateria.builder()
            .codigo(codigo)
            .sigla("PL")
            .numero("0042")
            .ano(2024)
            .ementa("Ementa " + codigo)
            .build();
    }

    @Test
    @DisplayName("INSERT: matéria nova é salva com hash e goldSincronizado=false")
    void inserirMateriaNova() {
        SilverSenadoMateria silver = novaMateria("111");
        when(hashGenerator.generate(silver)).thenReturn("hashNovo001");
        when(repository.findAllByCodigoIn(anyCollection())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverSenadoMateria> captor = ArgumentCaptor.forClass(SilverSenadoMateria.class);
        verify(repository).save(captor.capture());

        SilverSenadoMateria salvo = captor.getValue();
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo001");
        assertThat(salvo.isGoldSincronizado()).isFalse();
        assertThat(salvo.getEtlJobId()).isEqualTo(jobControl.getId());

        verify(etlMetrics).registrarInseridos(CasaLegislativa.SENADO, 1);
        verify(etlMetrics, never()).registrarAtualizados(any(), anyInt());
    }

    @Test
    @DisplayName("SKIP: matéria com mesmo hash não é salva novamente")
    void ignorarMateriaComHashIgual() {
        SilverSenadoMateria silver = novaMateria("222");
        SilverSenadoMateria existente = novaMateria("222");
        existente.setId(UUID.randomUUID());
        existente.setContentHash("hashIgual");
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashIgual");
        when(repository.findAllByCodigoIn(anyCollection())).thenReturn(List.of(existente));

        loader.carregar(List.of(silver), jobControl);

        verify(repository, never()).save(any());
        verify(etlMetrics).registrarIgnorados(CasaLegislativa.SENADO, 1);
    }

    @Test
    @DisplayName("UPDATE: matéria com hash diferente é atualizada com goldSincronizado=false")
    void atualizarMateriaComHashDiferente() {
        SilverSenadoMateria silver = novaMateria("333");
        UUID idExistente = UUID.randomUUID();
        LocalDateTime ingeridoEm = LocalDateTime.now().minusDays(3);

        SilverSenadoMateria existente = novaMateria("333");
        existente.setId(idExistente);
        existente.setContentHash("hashAntigo");
        existente.setIngeridoEm(ingeridoEm);
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashNovo");
        when(repository.findAllByCodigoIn(anyCollection())).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverSenadoMateria> captor = ArgumentCaptor.forClass(SilverSenadoMateria.class);
        verify(repository).save(captor.capture());

        SilverSenadoMateria salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(idExistente);
        assertThat(salvo.getIngeridoEm()).isEqualTo(ingeridoEm);
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo");
        assertThat(salvo.isGoldSincronizado()).isFalse();

        verify(etlMetrics).registrarAtualizados(CasaLegislativa.SENADO, 1);
    }

    @Test
    @DisplayName("lista vazia não realiza nenhuma operação")
    void listaVaziaNaoFazNada() {
        loader.carregar(List.of(), jobControl);
        verifyNoInteractions(repository, hashGenerator, etlMetrics);
    }

    @Test
    @DisplayName("erro em save incrementa contadorDeErros no jobControl")
    void erroEmSaveIncrementaErros() {
        SilverSenadoMateria silver = novaMateria("444");
        when(hashGenerator.generate(silver)).thenReturn("hash");
        when(repository.findAllByCodigoIn(anyCollection())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenThrow(new RuntimeException("Erro simulado"));

        loader.carregar(List.of(silver), jobControl);

        assertThat(jobControl.getTotalErros()).isEqualTo(1);
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
    }

    @Test
    @DisplayName("UPDATE: preserva campos de enriquecimento (det_*) ao atualizar")
    void atualizarMateriaPreservaCamposEnriquecimento() {
        SilverSenadoMateria silver = novaMateria("555");
        UUID idExistente = UUID.randomUUID();
        LocalDateTime ingeridoEm = LocalDateTime.now().minusDays(2);

        SilverSenadoMateria existente = novaMateria("555");
        existente.setId(idExistente);
        existente.setContentHash("hashAntigo");
        existente.setIngeridoEm(ingeridoEm);
        existente.setGoldSincronizado(true);
        // Campos de enriquecimento previamente preenchidos
        existente.setDetSiglaCasaIdentificacao("SF");
        existente.setDetIndicadorTramitando("Sim");
        existente.setDetIndexacao("licitação, contratos");
        existente.setDetNaturezaNome("Ordinária");
        existente.setDetSiglaCasaOrigem("SF");

        when(hashGenerator.generate(silver)).thenReturn("hashNovo");
        when(repository.findAllByCodigoIn(anyCollection())).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverSenadoMateria> captor = ArgumentCaptor.forClass(SilverSenadoMateria.class);
        verify(repository).save(captor.capture());

        SilverSenadoMateria salvo = captor.getValue();
        assertThat(salvo.getDetSiglaCasaIdentificacao()).isEqualTo("SF");
        assertThat(salvo.getDetIndicadorTramitando()).isEqualTo("Sim");
        assertThat(salvo.getDetIndexacao()).isEqualTo("licitação, contratos");
        assertThat(salvo.getDetNaturezaNome()).isEqualTo("Ordinária");
        assertThat(salvo.getDetSiglaCasaOrigem()).isEqualTo("SF");
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo");
        assertThat(salvo.isGoldSincronizado()).isFalse();
    }
}
