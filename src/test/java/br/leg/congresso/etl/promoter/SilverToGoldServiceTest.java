package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverToGoldService — orquestração de promoção Silver→Gold")
class SilverToGoldServiceTest {

    @Mock
    private SilverCamaraProposicaoRepository silverCamaraRepository;

    @Mock
    private SilverSenadoMateriaRepository silverSenadoRepository;

    @Mock
    private CamaraGoldPromoter camaraGoldPromoter;

    @Mock
    private SenadoGoldPromoter senadoGoldPromoter;

    @Mock
    private EtlJobControlService jobControlService;

    @InjectMocks
    private SilverToGoldService service;

    // ------------------------------------------------------------------ helpers

    @BeforeEach
    void setup() {
        // @Value não é injetado por @InjectMocks: batchSize fica 0, causaria loop infinito
        ReflectionTestUtils.setField(service, "batchSize", 500);
    }

    private EtlJobControl jobFor(CasaLegislativa casa) {
        return EtlJobControl.builder()
            .id(UUID.randomUUID())
            .origem(casa)
            .tipoExecucao(TipoExecucao.PROMOCAO)
            .iniciadoEm(LocalDateTime.now())
            .build();
    }

    private List<SilverCamaraProposicao> camaraRegs(int quantity) {
        List<SilverCamaraProposicao> list = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            SilverCamaraProposicao s = SilverCamaraProposicao.builder()
                .id(UUID.randomUUID())
                .camaraId(String.valueOf(i + 1))
                .siglaTipo("PL")
                .numero(i + 1)
                .ano(2024)
                .build();
            s.setGoldSincronizado(false);
            list.add(s);
        }
        return list;
    }

    private List<SilverSenadoMateria> senadoRegs(int quantity) {
        List<SilverSenadoMateria> list = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            SilverSenadoMateria s = SilverSenadoMateria.builder()
                .id(UUID.randomUUID())
                .codigo(String.valueOf(i + 1000))
                .sigla("PLS")
                .numero(String.valueOf(i + 1))
                .ano(2024)
                .build();
            s.setGoldSincronizado(false);
            list.add(s);
        }
        return list;
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("promoverCamara: chama camaraGoldPromoter com registros pendentes")
    void promoverCamara_processaRegistros() {
        List<SilverCamaraProposicao> pendentes = camaraRegs(3);
        EtlJobControl job = jobFor(CasaLegislativa.CAMARA);

        when(jobControlService.iniciar(eq(CasaLegislativa.CAMARA), eq(TipoExecucao.PROMOCAO), anyMap()))
            .thenReturn(job);
        when(silverCamaraRepository.findByGoldSincronizadoFalse()).thenReturn(pendentes);
        when(jobControlService.finalizar(job)).thenReturn(job);

        service.promoverCamara();

        ArgumentCaptor<List<SilverCamaraProposicao>> captor = ArgumentCaptor.forClass(List.class);
        verify(camaraGoldPromoter).promover(captor.capture(), eq(job));
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("promoverCamara: sem registros pendentes não chama promoter")
    void promoverCamara_semRegistrosPendentes_naoChama() {
        EtlJobControl job = jobFor(CasaLegislativa.CAMARA);

        when(jobControlService.iniciar(eq(CasaLegislativa.CAMARA), eq(TipoExecucao.PROMOCAO), anyMap()))
            .thenReturn(job);
        when(silverCamaraRepository.findByGoldSincronizadoFalse()).thenReturn(List.of());
        when(jobControlService.finalizar(job)).thenReturn(job);

        service.promoverCamara();

        verifyNoInteractions(camaraGoldPromoter);
    }

    @Test
    @DisplayName("promoverSenado: chama senadoGoldPromoter com registros pendentes")
    void promoverSenado_processaRegistros() {
        List<SilverSenadoMateria> pendentes = senadoRegs(2);
        EtlJobControl job = jobFor(CasaLegislativa.SENADO);

        when(jobControlService.iniciar(eq(CasaLegislativa.SENADO), eq(TipoExecucao.PROMOCAO), anyMap()))
            .thenReturn(job);
        when(silverSenadoRepository.findByGoldSincronizadoFalse()).thenReturn(pendentes);
        when(jobControlService.finalizar(job)).thenReturn(job);

        service.promoverSenado();

        ArgumentCaptor<List<SilverSenadoMateria>> captor = ArgumentCaptor.forClass(List.class);
        verify(senadoGoldPromoter).promover(captor.capture(), eq(job));
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("promoverTudo: chama ambas as casas em sequência")
    void promoverTudo_chamaAmbasCasas() {
        EtlJobControl jobCamara = jobFor(CasaLegislativa.CAMARA);
        EtlJobControl jobSenado = jobFor(CasaLegislativa.SENADO);

        when(jobControlService.iniciar(eq(CasaLegislativa.CAMARA), eq(TipoExecucao.PROMOCAO), anyMap()))
            .thenReturn(jobCamara);
        when(jobControlService.iniciar(eq(CasaLegislativa.SENADO), eq(TipoExecucao.PROMOCAO), anyMap()))
            .thenReturn(jobSenado);
        when(silverCamaraRepository.findByGoldSincronizadoFalse()).thenReturn(List.of());
        when(silverSenadoRepository.findByGoldSincronizadoFalse()).thenReturn(List.of());
        when(jobControlService.finalizar(jobCamara)).thenReturn(jobCamara);
        when(jobControlService.finalizar(jobSenado)).thenReturn(jobSenado);

        service.promoverTudo();

        verify(silverCamaraRepository).findByGoldSincronizadoFalse();
        verify(silverSenadoRepository).findByGoldSincronizadoFalse();
    }
}
