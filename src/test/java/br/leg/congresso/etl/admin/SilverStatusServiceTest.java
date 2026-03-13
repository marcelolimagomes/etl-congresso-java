package br.leg.congresso.etl.admin;

import br.leg.congresso.etl.admin.dto.SilverStatusDTO;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraTramitacaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMovimentacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverStatusService — contagens da camada Silver")
class SilverStatusServiceTest {

    @Mock private SilverCamaraDeputadoRepository camaraDeputadoRepository;
    @Mock private SilverCamaraProposicaoRepository camaraProposicaoRepository;
    @Mock private SilverCamaraTramitacaoRepository camaraTramitacaoRepository;
    @Mock private SilverSenadoMateriaRepository senadoMateriaRepository;
    @Mock private SilverSenadoMovimentacaoRepository senadoMovimentacaoRepository;
    @Mock private ProposicaoRepository proposicaoRepository;

    @InjectMocks
    private SilverStatusService service;

    @Test
    @DisplayName("deve retornar contagens corretas quando há registros")
    void calcularStatus_comRegistros_retornaContagens() {
        when(camaraDeputadoRepository.countDeputados()).thenReturn(7878L);
        when(camaraDeputadoRepository.countPendentesEnriquecimento()).thenReturn(0L);
        when(camaraDeputadoRepository.countComContatoEmail()).thenReturn(513L);
        when(camaraDeputadoRepository.countEmExercicioSemContatoEmail()).thenReturn(13L);
        when(camaraProposicaoRepository.count()).thenReturn(150L);
        when(camaraTramitacaoRepository.count()).thenReturn(3000L);
        when(senadoMateriaRepository.count()).thenReturn(80L);
        when(senadoMateriaRepository.countByDetSiglaCasaIdentificacaoIsNull()).thenReturn(2L);
        when(senadoMovimentacaoRepository.count()).thenReturn(2500L);
        when(proposicaoRepository.countByCasa(CasaLegislativa.CAMARA)).thenReturn(120L);
        when(proposicaoRepository.countByCasa(CasaLegislativa.SENADO)).thenReturn(70L);

        SilverStatusDTO status = service.calcularStatus(null);

        assertThat(status.anoFiltro()).isNull();
        assertThat(status.camaraDeputadosTotal()).isEqualTo(7878L);
        assertThat(status.camaraDeputadosPendentesEnriquecimento()).isZero();
        assertThat(status.camaraDeputadosComContatoEmail()).isEqualTo(513L);
        assertThat(status.camaraDeputadosEmExercicioSemContatoEmail()).isEqualTo(13L);
        assertThat(status.camaraProposicoesTotal()).isEqualTo(150L);
        assertThat(status.camaraTramitacoesTotal()).isEqualTo(3000L);
        assertThat(status.senadoMateriasTotal()).isEqualTo(80L);
        assertThat(status.senadoMateriasPendentesEnriquecimento()).isEqualTo(2L);
        assertThat(status.senadoMovimentacoesTotal()).isEqualTo(2500L);
        assertThat(status.goldCamaraProposicoesTotal()).isEqualTo(120L);
        assertThat(status.goldSenadoProposicoesTotal()).isEqualTo(70L);
        assertThat(status.isCamaraPendenteEnriquecimento()).isFalse();
        assertThat(status.isSenadoPendenteEnriquecimento()).isTrue();
    }

    @Test
    @DisplayName("deve retornar zeros quando não há registros")
    void calcularStatus_semRegistros_retornaZeros() {
        when(camaraDeputadoRepository.countDeputados()).thenReturn(0L);
        when(camaraDeputadoRepository.countPendentesEnriquecimento()).thenReturn(0L);
        when(camaraDeputadoRepository.countComContatoEmail()).thenReturn(0L);
        when(camaraDeputadoRepository.countEmExercicioSemContatoEmail()).thenReturn(0L);
        when(camaraProposicaoRepository.count()).thenReturn(0L);
        when(camaraTramitacaoRepository.count()).thenReturn(0L);
        when(senadoMateriaRepository.count()).thenReturn(0L);
        when(senadoMateriaRepository.countByDetSiglaCasaIdentificacaoIsNull()).thenReturn(0L);
        when(senadoMovimentacaoRepository.count()).thenReturn(0L);
        when(proposicaoRepository.countByCasa(CasaLegislativa.CAMARA)).thenReturn(0L);
        when(proposicaoRepository.countByCasa(CasaLegislativa.SENADO)).thenReturn(0L);

        SilverStatusDTO status = service.calcularStatus(null);

        assertThat(status.camaraDeputadosTotal()).isZero();
        assertThat(status.camaraDeputadosComContatoEmail()).isZero();
        assertThat(status.camaraProposicoesTotal()).isZero();
        assertThat(status.senadoMateriasPendentesEnriquecimento()).isZero();
        assertThat(status.goldCamaraProposicoesTotal()).isZero();
        assertThat(status.goldSenadoProposicoesTotal()).isZero();
        assertThat(status.isCamaraPendenteEnriquecimento()).isFalse();
        assertThat(status.isSenadoPendenteEnriquecimento()).isFalse();
    }

    @Test
    @DisplayName("deve aplicar filtro por ano e retornar Silver x Gold por casa")
    void calcularStatus_comFiltroAno_retornaContagensDoAno() {
        when(camaraDeputadoRepository.countDeputados()).thenReturn(7878L);
        when(camaraDeputadoRepository.countPendentesEnriquecimento()).thenReturn(0L);
        when(camaraDeputadoRepository.countComContatoEmail()).thenReturn(513L);
        when(camaraDeputadoRepository.countEmExercicioSemContatoEmail()).thenReturn(13L);
        when(camaraProposicaoRepository.countByAno(2024)).thenReturn(10L);
        when(camaraTramitacaoRepository.countByProposicaoAno(2024)).thenReturn(100L);
        when(senadoMateriaRepository.countByAno(2024)).thenReturn(12L);
        when(senadoMateriaRepository.countPendentesEnriquecimentoByAno(2024)).thenReturn(1L);
        when(senadoMovimentacaoRepository.countByMateriaAno(2024)).thenReturn(200L);
        when(proposicaoRepository.countByCasaAndAno(CasaLegislativa.CAMARA, 2024)).thenReturn(9L);
        when(proposicaoRepository.countByCasaAndAno(CasaLegislativa.SENADO, 2024)).thenReturn(11L);

        SilverStatusDTO status = service.calcularStatus(2024);

        assertThat(status.anoFiltro()).isEqualTo(2024);
        assertThat(status.camaraDeputadosTotal()).isEqualTo(7878L);
        assertThat(status.camaraDeputadosComContatoEmail()).isEqualTo(513L);
        assertThat(status.camaraProposicoesTotal()).isEqualTo(10L);
        assertThat(status.senadoMateriasTotal()).isEqualTo(12L);
        assertThat(status.senadoMateriasPendentesEnriquecimento()).isEqualTo(1L);
        assertThat(status.goldCamaraProposicoesTotal()).isEqualTo(9L);
        assertThat(status.goldSenadoProposicoesTotal()).isEqualTo(11L);
        assertThat(status.isSenadoPendenteEnriquecimento()).isTrue();
    }
}
