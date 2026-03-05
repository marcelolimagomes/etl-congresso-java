package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.loader.ProposicaoLoader;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.transformer.ProposicaoTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CamaraGoldPromoter — Silver→Gold transformação e rastreabilidade")
class CamaraGoldPromoterTest {

    @Mock
    private ProposicaoLoader proposicaoLoader;

    @Mock
    private ProposicaoTransformer transformer;

    @Mock
    private ProposicaoRepository proposicaoRepository;

    @Mock
    private SilverCamaraProposicaoRepository silverRepository;

    @InjectMocks
    private CamaraGoldPromoter promoter;

    private EtlJobControl jobControl;

    @BeforeEach
    void setup() {
        jobControl = EtlJobControl.builder()
            .id(UUID.randomUUID())
            .origem(CasaLegislativa.CAMARA)
            .tipoExecucao(TipoExecucao.PROMOCAO)
            .iniciadoEm(LocalDateTime.now())
            .build();

    }

    private SilverCamaraProposicao silverBase() {
        SilverCamaraProposicao silver = SilverCamaraProposicao.builder()
            .id(UUID.randomUUID())
            .camaraId("12345")
            .siglaTipo("PL")
            .numero(42)
            .ano(2024)
            .ementa("Ementa de teste")
            .dataApresentacao("2024-01-15")
            .ultimoStatusDescricaoSituacao("Em tramitação")
            .ultimoStatusDespacho("Encaminhado à CCJC")
            .ultimoStatusDataHora("2024-03-01T10:00:00")
            .urlInteiroTeor("http://camara.leg.br/PL-42-2024")
            .build();
        silver.setGoldSincronizado(false);
        return silver;
    }

    @Test
    @DisplayName("promover: converte Silver em Gold com campos mapeados corretamente")
    void converterSilverParaGold() {
        SilverCamaraProposicao silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        // Verifica que transformer foi chamado
        verify(transformer).enriquecerLote(anyList());

        // Captura a proposição passada ao ProposicaoLoader
        ArgumentCaptor<List<Proposicao>> captor = ArgumentCaptor.forClass(List.class);
        verify(proposicaoLoader).carregar(captor.capture(), eq(jobControl));

        List<Proposicao> proposicoes = captor.getValue();
        assertThat(proposicoes).hasSize(1);

        Proposicao gold = proposicoes.get(0);
        assertThat(gold.getCasa()).isEqualTo(CasaLegislativa.CAMARA);
        assertThat(gold.getSigla()).isEqualTo("PL");
        assertThat(gold.getNumero()).isEqualTo(42);
        assertThat(gold.getAno()).isEqualTo(2024);
        assertThat(gold.getEmenta()).isEqualTo("Ementa de teste");
        assertThat(gold.getIdOrigem()).isEqualTo("12345");
        assertThat(gold.getSilverCamaraId()).isEqualTo(silver.getId());
    }

    @Test
    @DisplayName("promover: marca Silver como goldSincronizado após promoção")
    void marcaSilverComoSincronizado() {
        SilverCamaraProposicao silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        verify(silverRepository).marcarGoldSincronizado(silver.getId());
    }

    @Test
    @DisplayName("promover: atualiza FK silverCamaraId na entidade Gold quando encontrada")
    void atualizaFkSilverNaEntidadeGold() {
        SilverCamaraProposicao silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Proposicao goldExistente = Proposicao.builder()
            .id(UUID.randomUUID())
            .casa(CasaLegislativa.CAMARA)
            .idOrigem("12345")
            .build();

        when(proposicaoRepository.findByCasaAndIdOrigem(CasaLegislativa.CAMARA, "12345"))
            .thenReturn(Optional.of(goldExistente));
        when(proposicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        ArgumentCaptor<Proposicao> captor = ArgumentCaptor.forClass(Proposicao.class);
        verify(proposicaoRepository).save(captor.capture());

        assertThat(captor.getValue().getSilverCamaraId()).isEqualTo(silver.getId());
    }

    @Test
    @DisplayName("promover: lista vazia não executa nenhuma operação")
    void listaVaziaNaoFazNada() {
        promoter.promover(List.of(), jobControl);
        verifyNoInteractions(proposicaoLoader, transformer, proposicaoRepository, silverRepository);
    }

    @Test
    @DisplayName("promover: usa ementaDetalhada quando ementa principal está vazia")
    void usaEmentaDetalhadaQuandoEmentaPrincipalEhVazia() {
        SilverCamaraProposicao silver = silverBase();
        silver.setEmenta("");
        silver.setEmentaDetalhada("Ementa detalhada substituta");
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        ArgumentCaptor<List<Proposicao>> captor = ArgumentCaptor.forClass(List.class);
        verify(proposicaoLoader).carregar(captor.capture(), any());

        assertThat(captor.getValue().get(0).getEmenta()).isEqualTo("Ementa detalhada substituta");
    }
}
