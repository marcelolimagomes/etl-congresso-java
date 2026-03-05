package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraApiExtractor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverEnrichmentService — enriquecimento Silver com fontes secundárias")
class SilverEnrichmentServiceTest {

    @Mock private SilverSenadoMateriaRepository silverSenadoRepository;
    @Mock private SilverCamaraProposicaoRepository silverCamaraRepository;
    @Mock private SenadoApiExtractor senadoApiExtractor;
    @Mock private CamaraApiExtractor camaraApiExtractor;
    @Mock private SilverCamaraTramitacaoLoader tramitacaoLoader;
    @Mock private SilverSenadoMovimentacaoLoader movimentacaoLoader;
    @Mock private EtlJobControlService jobControlService;

    @InjectMocks
    private SilverEnrichmentService service;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SilverSenadoMateria materiaComCodigo(String codigo) {
        return SilverSenadoMateria.builder()
            .id(UUID.randomUUID())
            .codigo(codigo)
            .sigla("PL")
            .numero("1")
            .ano(2024)
            .build();
    }

    private SilverCamaraProposicao proposicaoComId(String camaraId) {
        return SilverCamaraProposicao.builder()
            .id(UUID.randomUUID())
            .camaraId(camaraId)
            .siglaTipo("PL")
            .numero(1)
            .ano(2024)
            .build();
    }

    private SenadoDetalheDTO detalheDto(String siglaCasa) {
        SenadoDetalheDTO.DadosBasicos dados = new SenadoDetalheDTO.DadosBasicos();
        dados.setSiglaCasaIdentificacao(siglaCasa);
        dados.setSiglaSubtipo("PL");
        dados.setDescricaoSubtipo("Projeto de Lei");
        dados.setIndicadorTramitando("Sim");
        dados.setSiglaCasaIniciadora("SF");

        SenadoDetalheDTO.Natureza natureza = new SenadoDetalheDTO.Natureza();
        natureza.setCodigoNatureza("1");
        natureza.setNomeNatureza("Ordinária");

        SenadoDetalheDTO.CasaOrigem casaOrigem = new SenadoDetalheDTO.CasaOrigem();
        casaOrigem.setSiglaCasaOrigem("SF");

        SenadoDetalheDTO.MateriaDetalhe materiaDetalhe = new SenadoDetalheDTO.MateriaDetalhe();
        materiaDetalhe.setDadosBasicosMateria(dados);
        materiaDetalhe.setNaturezaMateria(natureza);
        materiaDetalhe.setCasaOrigem(casaOrigem);

        SenadoDetalheDTO.DetalheMateria detalhe = new SenadoDetalheDTO.DetalheMateria();
        detalhe.setMateria(materiaDetalhe);

        SenadoDetalheDTO dto = new SenadoDetalheDTO();
        dto.setDetalheMateria(detalhe);
        return dto;
    }

    // ─── Testes de enriquecerDetalhesSenado ───────────────────────────────────

    @Test
    @DisplayName("deve enriquecer registros Silver Senado com campos det_*")
    void enriquecerDetalhesSenado_populaCamposDet() {
        SilverSenadoMateria materia = materiaComCodigo("12345");
        SenadoDetalheDTO detalhe = detalheDto("SF");

        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
            .thenReturn(List.of(materia));
        when(senadoApiExtractor.fetchRawDetalhe("12345")).thenReturn(detalhe);
        when(silverSenadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(1);
        assertThat(materia.getDetSiglaCasaIdentificacao()).isEqualTo("SF");
        assertThat(materia.getDetSiglaSubtipo()).isEqualTo("PL");
        assertThat(materia.getDetNaturezaNome()).isEqualTo("Ordinária");
        assertThat(materia.getDetSiglaCasaOrigem()).isEqualTo("SF");
        verify(silverSenadoRepository).save(materia);
    }

    @Test
    @DisplayName("não deve chamar extrator quando não há registros pendentes")
    void enriquecerDetalhesSenado_semPendentes_naoChama() {
        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
            .thenReturn(Collections.emptyList());

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(0);
        verifyNoInteractions(senadoApiExtractor);
        verify(silverSenadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("erro em um registro não deve interromper processamento dos demais")
    void enriquecerDetalhesSenado_erroNaoInterrompeLote() {
        SilverSenadoMateria ok1 = materiaComCodigo("111");
        SilverSenadoMateria erro = materiaComCodigo("999");
        SilverSenadoMateria ok2 = materiaComCodigo("222");

        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
            .thenReturn(List.of(ok1, erro, ok2));
        when(senadoApiExtractor.fetchRawDetalhe("111")).thenReturn(detalheDto("SF"));
        when(senadoApiExtractor.fetchRawDetalhe("999")).thenThrow(new RuntimeException("API Error"));
        when(senadoApiExtractor.fetchRawDetalhe("222")).thenReturn(detalheDto("CD"));
        when(silverSenadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(2);
        verify(silverSenadoRepository, times(2)).save(any());
    }

    // ─── Testes de enriquecerTramitacoesCamara ────────────────────────────────

    @Test
    @DisplayName("deve chamar loader para cada proposição e somar inserções")
    void enriquecerTramitacoesCamara_chamaLoader() {
        SilverCamaraProposicao prop1 = proposicaoComId("100");
        SilverCamaraProposicao prop2 = proposicaoComId("200");
        List<CamaraTramitacaoDTO> trams1 = List.of(new CamaraTramitacaoDTO());
        List<CamaraTramitacaoDTO> trams2 = List.of(new CamaraTramitacaoDTO(), new CamaraTramitacaoDTO());

        when(camaraApiExtractor.fetchTramitacoesRaw("100")).thenReturn(trams1);
        when(camaraApiExtractor.fetchTramitacoesRaw("200")).thenReturn(trams2);
        when(tramitacaoLoader.carregar(eq(prop1), any())).thenReturn(1);
        when(tramitacaoLoader.carregar(eq(prop2), any())).thenReturn(2);

        int total = service.enriquecerTramitacoesCamara(List.of(prop1, prop2));

        assertThat(total).isEqualTo(3);
        verify(tramitacaoLoader).carregar(prop1, trams1);
        verify(tramitacaoLoader).carregar(prop2, trams2);
    }

    @Test
    @DisplayName("deve usar findAll quando lista de proposições não é fornecida")
    void enriquecerTramitacoesCamara_usaFindAllQuandoNull() {
        SilverCamaraProposicao prop = proposicaoComId("100");
        when(silverCamaraRepository.findAll()).thenReturn(List.of(prop));
        when(camaraApiExtractor.fetchTramitacoesRaw("100")).thenReturn(Collections.emptyList());
        when(tramitacaoLoader.carregar(any(), any())).thenReturn(0);

        service.enriquecerTramitacoesCamara(null);

        verify(silverCamaraRepository).findAll();
        verify(camaraApiExtractor).fetchTramitacoesRaw("100");
    }

    // ─── Testes de enriquecerMovimentacoesSenado ──────────────────────────────

    @Test
    @DisplayName("deve chamar loader para cada matéria e somar inserções")
    void enriquecerMovimentacoesSenado_chamaLoader() {
        SilverSenadoMateria mat1 = materiaComCodigo("AAA");
        SilverSenadoMateria mat2 = materiaComCodigo("BBB");
        List<SenadoMovimentacaoDTO.Movimentacao> movs = List.of(new SenadoMovimentacaoDTO.Movimentacao());

        when(senadoApiExtractor.fetchRawMovimentacoes("AAA")).thenReturn(movs);
        when(senadoApiExtractor.fetchRawMovimentacoes("BBB")).thenReturn(movs);
        when(movimentacaoLoader.carregar(eq(mat1), any())).thenReturn(3);
        when(movimentacaoLoader.carregar(eq(mat2), any())).thenReturn(2);

        int total = service.enriquecerMovimentacoesSenado(List.of(mat1, mat2));

        assertThat(total).isEqualTo(5);
        verify(movimentacaoLoader).carregar(mat1, movs);
        verify(movimentacaoLoader).carregar(mat2, movs);
    }

    // ─── Teste de applyDetalhe (método público auxiliar) ─────────────────────

    @Test
    @DisplayName("applyDetalhe deve pular graciosamente detalhe nulo")
    void applyDetalhe_comDtoNulo_naoFazNada() {
        SilverSenadoMateria materia = materiaComCodigo("999");

        service.applyDetalhe(materia, null);

        assertThat(materia.getDetSiglaCasaIdentificacao()).isNull();
        assertThat(materia.getDetSiglaSubtipo()).isNull();
    }

    @Test
    @DisplayName("applyDetalhe deve usar nomeCasaIniciadora quando siglaCasaIniciadora for vazia")
    void applyDetalhe_usaNomeCasaQuandoSiglaVazia() {
        SilverSenadoMateria materia = materiaComCodigo("123");
        SenadoDetalheDTO detalhe = detalheDto("SF");
        detalhe.getDetalheMateria().getMateria().getDadosBasicosMateria().setSiglaCasaIniciadora("");
        detalhe.getDetalheMateria().getMateria().getDadosBasicosMateria().setNomeCasaIniciadora("Senado Federal");

        service.applyDetalhe(materia, detalhe);

        assertThat(materia.getDetCasaIniciadora()).isEqualTo("Senado Federal");
    }
}
