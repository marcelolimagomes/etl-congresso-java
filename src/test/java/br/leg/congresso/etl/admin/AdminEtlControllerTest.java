package br.leg.congresso.etl.admin;

import br.leg.congresso.etl.admin.dto.SilverStatusDTO;
import br.leg.congresso.etl.orchestrator.EtlOrchestrator;
import br.leg.congresso.etl.service.EtlJobControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminEtlController — endpoint de status Silver")
class AdminEtlControllerTest {

    @Mock
    private EtlOrchestrator orchestrator;

    @Mock
    private EtlJobControlService jobControlService;

    @Mock
    private SilverStatusService silverStatusService;

    @InjectMocks
    private AdminEtlController controller;

    @Test
    @DisplayName("deve retornar status Silver x Gold com filtro de ano")
    void silverStatus_comAno_retornaPayload() {
        SilverStatusDTO dto = new SilverStatusDTO(
            2024,
            5419,
            12000,
            1431,
            10,
            4800,
            5300,
            1400
        );
        when(silverStatusService.calcularStatus(2024)).thenReturn(dto);

        ResponseEntity<SilverStatusDTO> response = controller.silverStatus(2024);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().anoFiltro()).isEqualTo(2024);
        assertThat(response.getBody().goldCamaraProposicoesTotal()).isEqualTo(5300);
        verify(silverStatusService).calcularStatus(2024);
    }
}
