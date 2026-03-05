package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.loader.ProposicaoLoader;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
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
@DisplayName("SenadoGoldPromoter — Silver→Gold transformação e rastreabilidade (Senado)")
class SenadoGoldPromoterTest {

    @Mock
    private ProposicaoLoader proposicaoLoader;

    @Mock
    private ProposicaoTransformer transformer;

    @Mock
    private ProposicaoRepository proposicaoRepository;

    @Mock
    private SilverSenadoMateriaRepository silverRepository;

    @InjectMocks
    private SenadoGoldPromoter promoter;

    private EtlJobControl jobControl;

    @BeforeEach
    void setup() {
        jobControl = EtlJobControl.builder()
            .id(UUID.randomUUID())
            .origem(CasaLegislativa.SENADO)
            .tipoExecucao(TipoExecucao.PROMOCAO)
            .iniciadoEm(LocalDateTime.now())
            .build();
    }

    private SilverSenadoMateria silverBase() {
        SilverSenadoMateria silver = SilverSenadoMateria.builder()
            .id(UUID.randomUUID())
            .codigo("123456")
            .sigla("PL")
            .numero("100")
            .ano(2024)
            .ementa("Dispõe sobre normas de teste")
            .data("15/02/2024")
            .autor("Senador Fulano de Tal")
            .urlDetalheMateria("https://legis.senado.leg.br/sdleg-getter/documento?dm=123456")
            .build();
        silver.setGoldSincronizado(false);
        return silver;
    }

    @Test
    @DisplayName("promover: converte Silver em Gold com campos mapeados corretamente")
    void converterSilverParaGold() {
        SilverSenadoMateria silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        verify(transformer).enriquecerLote(anyList());

        ArgumentCaptor<List<Proposicao>> captor = ArgumentCaptor.forClass(List.class);
        verify(proposicaoLoader).carregar(captor.capture(), eq(jobControl));

        List<Proposicao> proposicoes = captor.getValue();
        assertThat(proposicoes).hasSize(1);

        Proposicao gold = proposicoes.get(0);
        assertThat(gold.getCasa()).isEqualTo(CasaLegislativa.SENADO);
        assertThat(gold.getSigla()).isEqualTo("PL");
        assertThat(gold.getAno()).isEqualTo(2024);
        assertThat(gold.getEmenta()).isEqualTo("Dispõe sobre normas de teste");
        assertThat(gold.getIdOrigem()).isEqualTo("123456");
        assertThat(gold.getSilverSenadoId()).isEqualTo(silver.getId());
    }

    @Test
    @DisplayName("promover: marca Silver como goldSincronizado após promoção")
    void marcaSilverComoSincronizado() {
        SilverSenadoMateria silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        verify(silverRepository).marcarGoldSincronizado(silver.getId());
    }

    @Test
    @DisplayName("promover: atualiza FK silverSenadoId na entidade Gold quando encontrada")
    void atualizaFkSilverNaEntidadeGold() {
        SilverSenadoMateria silver = silverBase();
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Proposicao goldExistente = Proposicao.builder()
            .id(UUID.randomUUID())
            .casa(CasaLegislativa.SENADO)
            .idOrigem("123456")
            .build();

        when(proposicaoRepository.findByCasaAndIdOrigem(CasaLegislativa.SENADO, "123456"))
            .thenReturn(Optional.of(goldExistente));
        when(proposicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        ArgumentCaptor<Proposicao> captor = ArgumentCaptor.forClass(Proposicao.class);
        verify(proposicaoRepository).save(captor.capture());
        assertThat(captor.getValue().getSilverSenadoId()).isEqualTo(silver.getId());
    }

    @Test
    @DisplayName("promover: lista vazia não executa nenhuma operação")
    void listaVaziaNaoFazNada() {
        promoter.promover(List.of(), jobControl);
        verifyNoInteractions(proposicaoLoader, transformer, proposicaoRepository, silverRepository);
    }

    @Test
    @DisplayName("promover: situação usa detIndicadorTramitando do enriquecimento")
    void situacaoUsaDetIndicadorTramitando() {
        SilverSenadoMateria silver = silverBase();
        silver.setDetIndicadorTramitando("Sim");
        silver.setDetIndexacao("licitação, contratos");
        when(transformer.enriquecerLote(anyList())).thenAnswer(inv -> inv.getArgument(0));

        promoter.promover(List.of(silver), jobControl);

        ArgumentCaptor<List<Proposicao>> captor = ArgumentCaptor.forClass(List.class);
        verify(proposicaoLoader).carregar(captor.capture(), any());

        Proposicao gold = captor.getValue().get(0);
        assertThat(gold.getSituacao()).isEqualTo("Sim");
        assertThat(gold.getKeywords()).isEqualTo("licitação, contratos");
    }
}
