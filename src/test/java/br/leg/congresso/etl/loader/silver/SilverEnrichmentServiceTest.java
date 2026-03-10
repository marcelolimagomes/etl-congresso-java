package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverEnrichmentService — enriquecimento Silver com fontes secundárias")
class SilverEnrichmentServiceTest {

    @Mock
    private SilverSenadoMateriaRepository silverSenadoRepository;
    @Mock
    private SilverCamaraProposicaoRepository silverCamaraRepository;
    @Mock
    private SenadoApiExtractor senadoApiExtractor;
    @Mock
    private CamaraApiExtractor camaraApiExtractor;
    @Mock
    private SilverCamaraTramitacaoLoader tramitacaoLoader;
    @Mock
    private SilverSenadoMovimentacaoLoader movimentacaoLoader;
    @Mock
    private SilverSenadoAutoriaLoader autoriaLoader;
    @Mock
    private SilverSenadoRelatoriaLoader relatoriaLoader;
    @Mock
    private SilverCamaraRelacionadasLoader relacionadasLoader;
    @Mock
    private SilverSenadoEmendaLoader emendaLoader;
    @Mock
    private SilverSenadoDocumentoLoader documentoLoader;
    @Mock
    private SilverSenadoPrazoLoader prazoLoader;
    @Mock
    private SilverSenadoVotacaoLoader votacaoLoader;
    @Mock
    private EtlJobControlService jobControlService;
    @Mock
    private SilverSenadoEnriquecedor senadoEnriquecedor;

    /**
     * Executor síncrono: executa runnable inline, sem threads extras — ideal para
     * testes.
     */
    private final Executor directExecutor = Runnable::run;

    private SilverEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new SilverEnrichmentService(
                silverSenadoRepository,
                silverCamaraRepository,
                senadoApiExtractor,
                camaraApiExtractor,
                tramitacaoLoader,
                movimentacaoLoader,
                autoriaLoader,
                relatoriaLoader,
                relacionadasLoader,
                emendaLoader,
                documentoLoader,
                prazoLoader,
                votacaoLoader,
                jobControlService,
                new ObjectMapper(),
                directExecutor,
                senadoEnriquecedor);
    }

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
        SenadoDetalheDTO.IdentificacaoMateria identif = new SenadoDetalheDTO.IdentificacaoMateria();
        identif.setSiglaCasaIdentificacao(siglaCasa);
        identif.setSiglaSubtipo("PL");
        identif.setDescricaoSubtipo("Projeto de Lei");
        identif.setIndicadorTramitando("Sim");

        SenadoDetalheDTO.DadosBasicos dados = new SenadoDetalheDTO.DadosBasicos();
        dados.setSiglaCasaIniciadora("SF");

        SenadoDetalheDTO.Natureza natureza = new SenadoDetalheDTO.Natureza();
        natureza.setCodigoNatureza("1");
        natureza.setNomeNatureza("Ordinária");

        SenadoDetalheDTO.CasaOrigem casaOrigem = new SenadoDetalheDTO.CasaOrigem();
        casaOrigem.setSiglaCasaOrigem("SF");

        SenadoDetalheDTO.MateriaDetalhe materiaDetalhe = new SenadoDetalheDTO.MateriaDetalhe();
        materiaDetalhe.setIdentificacaoMateria(identif);
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
        // Simula enriquecedor gerenciado: chama o consumer na entidade e retorna true
        doAnswer(invocation -> {
            Consumer<SilverSenadoMateria> enricher = invocation.getArgument(1);
            enricher.accept(materia);
            return true;
        }).when(senadoEnriquecedor).enriquecer(eq("12345"), any());

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(1);
        assertThat(materia.getDetSiglaCasaIdentificacao()).isEqualTo("SF");
        assertThat(materia.getDetSiglaSubtipo()).isEqualTo("PL");
        assertThat(materia.getDetNaturezaNome()).isEqualTo("Ordinária");
        assertThat(materia.getDetSiglaCasaOrigem()).isEqualTo("SF");
        verify(senadoEnriquecedor).enriquecer(eq("12345"), any());
    }

    @Test
    @DisplayName("não deve chamar extrator quando não há registros pendentes")
    void enriquecerDetalhesSenado_semPendentes_naoChama() {
        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
                .thenReturn(Collections.emptyList());

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(0);
        verifyNoInteractions(senadoApiExtractor);
        verifyNoInteractions(senadoEnriquecedor);
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
        when(senadoEnriquecedor.enriquecer(any(), any())).thenReturn(true);

        int resultado = service.enriquecerDetalhesSenado();

        assertThat(resultado).isEqualTo(2);
        verify(senadoEnriquecedor, times(2)).enriquecer(any(), any());
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

    @Test
    @DisplayName("applyDetalhe deve aceitar nomeCasaIniciadora com mais de 10 caracteres sem truncamento (fix 6.2)")
    void applyDetalhe_nomeCasaIniciadoraLonga_naoTrunca() {
        SilverSenadoMateria materia = materiaComCodigo("456");
        SenadoDetalheDTO detalhe = detalheDto("SF");
        detalhe.getDetalheMateria().getMateria().getDadosBasicosMateria().setSiglaCasaIniciadora(null);
        detalhe.getDetalheMateria().getMateria().getDadosBasicosMateria().setNomeCasaIniciadora("Câmara dos Deputados");

        service.applyDetalhe(materia, detalhe);

        // Campo deve conter o valor completo — sem truncamento nos 10 primeiros chars
        assertThat(materia.getDetCasaIniciadora()).isEqualTo("Câmara dos Deputados");
        assertThat(materia.getDetCasaIniciadora().length()).isGreaterThan(10);
    }

    // ─── Testes de enriquecerTudo (Phase 9) ──────────────────────────────────

    @Test
    @DisplayName("enriquecerTudo deve invocar todos os enriquecimentos e retornar job finalizado")
    void enriquecerTudo_invocaTodosOsMetodos() {
        // repositories retornam listas vazias — nenhuma API real é chamada
        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
                .thenReturn(Collections.emptyList());
        when(silverCamaraRepository.findAll())
                .thenReturn(Collections.emptyList());
        when(silverSenadoRepository.findAll())
                .thenReturn(Collections.emptyList());
        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNotNull())
                .thenReturn(Collections.emptyList());

        EtlJobControl job = EtlJobControl.builder()
                .id(UUID.randomUUID())
                .build();
        when(jobControlService.iniciar(
                eq(CasaLegislativa.SENADO),
                eq(TipoExecucao.ENRICHMENT),
                any(Map.class)))
                .thenReturn(job);
        when(jobControlService.finalizar(job)).thenReturn(job);

        EtlJobControl resultado = service.enriquecerTudo();

        assertThat(resultado).isEqualTo(job);
        // Job lifecycle
        verify(jobControlService).iniciar(eq(CasaLegislativa.SENADO), eq(TipoExecucao.ENRICHMENT), any(Map.class));
        verify(jobControlService).finalizar(job);

        // Todas as consultas aos repositórios devem ter sido feitas
        verify(silverSenadoRepository).findByDetSiglaCasaIdentificacaoIsNull();
        verify(silverCamaraRepository, times(2)).findAll(); // tramitacoes + relacionadas
        verify(silverSenadoRepository).findAll(); // movimentacoes
        // Metodos que consultam findByDetSiglaCasaIdentificacaoIsNotNull:
        // autorias + relatorias + emendas + documentos + prazos + votacoes = 6x
        verify(silverSenadoRepository, times(6)).findByDetSiglaCasaIdentificacaoIsNotNull();
    }

    @Test
    @DisplayName("enriquecerTudo deve marcar job como falho quando ocorre exceção")
    void enriquecerTudo_excecaoMarcaJobFalho() {
        EtlJobControl job = EtlJobControl.builder()
                .id(UUID.randomUUID())
                .build();
        when(jobControlService.iniciar(any(), any(), any())).thenReturn(job);
        when(silverSenadoRepository.findByDetSiglaCasaIdentificacaoIsNull())
                .thenThrow(new RuntimeException("DB error"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.enriquecerTudo());

        verify(jobControlService).falhar(eq(job), any());
        verify(jobControlService, never()).finalizar(any());
    }
}
