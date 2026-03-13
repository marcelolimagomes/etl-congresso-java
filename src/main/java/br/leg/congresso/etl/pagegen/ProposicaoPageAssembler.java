package br.leg.congresso.etl.pagegen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoRelacionada;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;
import br.leg.congresso.etl.domain.silver.SilverCamaraVotacao;
import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoDocumento;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoVotacao;
import br.leg.congresso.etl.pagegen.dto.AutorDTO;
import br.leg.congresso.etl.pagegen.dto.DocumentoResumoDTO;
import br.leg.congresso.etl.pagegen.dto.ProposicaoPageDTO;
import br.leg.congresso.etl.pagegen.dto.ProposicaoResumoDTO;
import br.leg.congresso.etl.pagegen.dto.TramitacaoDTO;
import br.leg.congresso.etl.pagegen.dto.VotacaoResumoDTO;
import br.leg.congresso.etl.repository.TramitacaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRelacionadaRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoTemaRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoDocumentoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoVotacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Monta o {@link ProposicaoPageDTO} a partir das entidades Gold + Silver.
 *
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Consultar dados Gold ({@code Proposicao} + {@code Tramitacao})</li>
 * <li>Consultar dados Silver (autores, temas — via FK traçada no Gold)</li>
 * <li>Formatar datas, construir textos SEO e gerar JSON-LD</li>
 * </ul>
 *
 * <p>
 * Sem lógica de I/O — a gravação do HTML é responsabilidade do
 * {@link PageGeneratorService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProposicaoPageAssembler {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String BASE_URL = "https://www.translegis.com.br";
    private static final String SITE_NAME = "Transparência Legislativa";

    private final TramitacaoRepository tramitacaoRepository;
    private final SilverCamaraProposicaoRepository silverCamaraProposicaoRepository;
    private final SilverCamaraProposicaoAutorRepository silverCamaraAutorRepository;
    private final SilverCamaraProposicaoTemaRepository silverCamaraProposicaoTemaRepository;
    private final SilverCamaraProposicaoRelacionadaRepository silverCamaraProposicaoRelacionadaRepository;
    private final SilverCamaraVotacaoRepository silverCamaraVotacaoRepository;
    private final SilverSenadoMateriaRepository silverSenadoMateriaRepository;
    private final SilverSenadoAutoriaRepository silverSenadoAutoriaRepository;
    private final SilverSenadoDocumentoRepository silverSenadoDocumentoRepository;
    private final SilverSenadoVotacaoRepository silverSenadoVotacaoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Monta o DTO completo para a proposição informada.
     *
     * @param proposicao entidade Gold já carregada
     * @return DTO pronto para renderização
     */
    public ProposicaoPageDTO assemble(Proposicao proposicao) {
        var tramitacoes = tramitacaoRepository.findByProposicaoIdOrdered(proposicao.getId());
        boolean isCamara = proposicao.getCasa() == CasaLegislativa.CAMARA;

        var autores = isCamara
                ? buildAutoresCamara(proposicao)
                : buildAutoresSenado(proposicao);

        var temas = isCamara
                ? buildTemasCamara(proposicao)
                : buildTemasSenadoFromIndexacao(proposicao);

        String ementaDetalhada = isCamara
                ? resolveEmentaDetalhadaCamara(proposicao)
                : resolveIndexacaoSenado(proposicao);

        String regime = isCamara
                ? resolveRegimeCamara(proposicao)
                : null;

        String casaStr = isCamara ? "camara" : "senado";
        String casaLabel = isCamara ? "Câmara dos Deputados" : "Senado Federal";
        String idOriginal = proposicao.getIdOrigem();
        String siglaTipo = proposicao.getSigla() != null ? proposicao.getSigla() : proposicao.getTipo().getSigla();
        String descricaoTipo = resolveDescricaoTipo(proposicao);
        String situacaoDescricao = normalizeSituacaoDescricao(proposicao.getSituacao(), proposicao.getCasa());

        String autoriaResumo = buildAutoriaResumo(autores);
        String dataApresentacaoFormatada = formatDate(proposicao.getDataApresentacao());
        String dataApresentacaoIso = proposicao.getDataApresentacao() != null
                ? proposicao.getDataApresentacao().format(ISO_DATE)
                : null;
        String dataAtualizacaoIso = proposicao.getDataAtualizacao() != null
                ? proposicao.getDataAtualizacao().format(ISO_DT)
                : null;

        String canonicalUrl = BASE_URL + "/stat-proposicoes/" + casaStr + "-" + idOriginal + "/";
        String situacaoTramitacao = resolveSituacaoTramitacao(proposicao, situacaoDescricao);

        String seoTitle = buildSeoTitle(siglaTipo, proposicao.getNumero(), proposicao.getAno(), casaLabel);
        String seoDescription = buildSeoDescription(proposicao.getEmenta(), autores);

        String schemaLegislation = buildSchemaLegislation(proposicao, canonicalUrl, siglaTipo, casaLabel,
                dataApresentacaoIso, dataAtualizacaoIso);
        String schemaBreadcrumb = buildSchemaBreadcrumb(canonicalUrl, siglaTipo,
                proposicao.getNumero(), proposicao.getAno());

        var tramitacaoDTOs = buildTramitacoes(tramitacoes);
        var documentos = buildDocumentos(proposicao);
        var relacionadas = isCamara
            ? buildRelacionadasCamara(proposicao)
            : List.<ProposicaoResumoDTO>of();
        var votacoes = isCamara
            ? buildVotacoesCamara(proposicao)
            : buildVotacoesSenado(proposicao);
        var keywords = parseKeywords(proposicao.getKeywords());
        var orgaoAtual = resolveOrgaoAtual(proposicao);
        var proposicaoJsonEmbutido = buildProposicaoJson(
                casaStr, idOriginal, siglaTipo, descricaoTipo, proposicao,
                autores, tramitacoes, temas, ementaDetalhada, orgaoAtual, regime,
                autoriaResumo, situacaoDescricao, situacaoTramitacao, dataApresentacaoIso, dataAtualizacaoIso,
                keywords);

        return ProposicaoPageDTO.builder()
                .casa(casaStr)
                .casaLabel(casaLabel)
                .idOriginal(idOriginal)
                .siglaTipo(siglaTipo)
                .descricaoTipo(descricaoTipo)
                .numero(proposicao.getNumero() != null ? String.valueOf(proposicao.getNumero()) : null)
                .ano(proposicao.getAno())
                .ementa(proposicao.getEmenta())
                .ementaDetalhada(ementaDetalhada)
                .situacaoDescricao(situacaoDescricao)
                .situacaoTramitacao(situacaoTramitacao)
                .dataApresentacao(dataApresentacaoFormatada)
                .orgaoAtual(orgaoAtual)
                .regime(regime)
                .urlInteiroTeor(proposicao.getUrlInteiroTeor())
                .keywords(keywords)
                .temas(temas)
                .autoriaResumo(autoriaResumo)
                .autores(autores)
                .tramitacoes(tramitacaoDTOs)
                .documentos(documentos)
                .relacionadas(relacionadas)
                .votacoes(votacoes)
                .canonicalUrl(canonicalUrl)
                .seoTitle(seoTitle)
                .seoDescription(seoDescription)
                .schemaOrgLegislationJson(schemaLegislation)
                .schemaOrgBreadcrumbJson(schemaBreadcrumb)
                .dataPublicacaoIso(dataApresentacaoIso)
                .dataAtualizacaoIso(dataAtualizacaoIso)
                .geradoEm(LocalDateTime.now().format(ISO_DT))
                .proposicaoJsonEmbutido(proposicaoJsonEmbutido)
                .build();
    }

    // ── Autores ───────────────────────────────────────────────────────────────

    private List<AutorDTO> buildAutoresCamara(Proposicao proposicao) {
        if (proposicao.getSilverCamaraId() == null)
            return List.of();
        var autores = silverCamaraAutorRepository
                .findByCamaraProposicaoIdOrderByOrdemAssinaturaAsc(proposicao.getSilverCamaraId());
        return autores.stream()
                .map(this::toAutorDTOCamara)
                .toList();
    }

    private AutorDTO toAutorDTOCamara(SilverCamaraProposicaoAutor a) {
        boolean isParlamentar = a.getIdDeputadoAutor() != null && !a.getIdDeputadoAutor().isBlank();
        return AutorDTO.builder()
                .nome(a.getNomeAutor())
                .tipo(a.getTipoAutor())
                .casa(isParlamentar ? "camara" : null)
                .idOriginal(isParlamentar ? a.getIdDeputadoAutor() : null)
                .proponente(Integer.valueOf(1).equals(a.getProponente()))
                .partido(a.getSiglaPartidoAutor())
                .uf(a.getSiglaUfAutor())
                .build();
    }

    private List<AutorDTO> buildAutoresSenado(Proposicao proposicao) {
        if (proposicao.getSilverSenadoId() == null)
            return List.of();
        var autores = silverSenadoAutoriaRepository.findBySenadoMateriaId(proposicao.getSilverSenadoId());
        return autores.stream()
                .map(this::toAutorDTOSenado)
                .toList();
    }

    private AutorDTO toAutorDTOSenado(SilverSenadoAutoria a) {
        boolean isParlamentar = a.getCodigoParlamentar() != null && !a.getCodigoParlamentar().isBlank();
        return AutorDTO.builder()
                .nome(a.getNomeAutor())
                .tipo(a.getDescricaoTipoAutor())
                .casa(isParlamentar ? "senado" : null)
                .idOriginal(isParlamentar ? a.getCodigoParlamentar() : null)
                .proponente(false)
                .partido(a.getSiglaPartido())
                .uf(a.getUfParlamentar())
                .build();
    }

    // ── Temas ─────────────────────────────────────────────────────────────────

    private List<String> buildTemasCamara(Proposicao proposicao) {
        if (proposicao.getSilverCamaraId() == null)
            return List.of();
        return silverCamaraProposicaoTemaRepository
                .findByCamaraProposicaoId(proposicao.getSilverCamaraId())
                .stream()
                .map(SilverCamaraProposicaoTema::getTema)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
    }

    private List<String> buildTemasSenadoFromIndexacao(Proposicao proposicao) {
        if (proposicao.getSilverSenadoId() == null)
            return List.of();
        var silverSenadoId = proposicao.getSilverSenadoId();
        return silverSenadoMateriaRepository.findById(java.util.Objects.requireNonNull(silverSenadoId))
                .map(SilverSenadoMateria::getDetIndexacao)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> List.of(s.split("[,;]+\\s*")))
                .orElse(List.of())
                .stream()
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .limit(10)
                .toList();
    }

    private List<ProposicaoResumoDTO> buildRelacionadasCamara(Proposicao proposicao) {
        if (proposicao.getIdOrigem() == null) {
            return List.of();
        }
        return silverCamaraProposicaoRelacionadaRepository.findByProposicaoId(proposicao.getIdOrigem()).stream()
                .sorted(java.util.Comparator
                        .comparing(this::parseAnoRelacionada, java.util.Comparator.reverseOrder())
                        .thenComparing(SilverCamaraProposicaoRelacionada::getRelacionadaNumero,
                                java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toProposicaoRelacionadaResumo)
                .filter(java.util.Objects::nonNull)
                .limit(8)
                .toList();
    }

    private List<VotacaoResumoDTO> buildVotacoesCamara(Proposicao proposicao) {
        if (proposicao.getSilverCamaraId() == null) {
            return List.of();
        }
        Integer idCamara = resolveCamaraId(proposicao);
        if (idCamara == null) {
            return List.of();
        }
        return silverCamaraVotacaoRepository
                .findByUltimaApresentacaoProposicaoIdProposicaoOrderByDataDesc(idCamara)
                .stream()
                .map(this::toVotacaoResumo)
                .limit(8)
                .toList();
    }

    private List<VotacaoResumoDTO> buildVotacoesSenado(Proposicao proposicao) {
        if (proposicao.getSilverSenadoId() == null) {
            return List.of();
        }
        return silverSenadoVotacaoRepository.findBySenadoMateriaId(proposicao.getSilverSenadoId()).stream()
                .sorted(Comparator.comparing(this::parseDataSessaoSenado,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toVotacaoResumo)
                .limit(8)
                .toList();
    }

    private List<DocumentoResumoDTO> buildDocumentos(Proposicao proposicao) {
        Map<String, DocumentoResumoDTO> documentos = new LinkedHashMap<>();

        if (notBlank(proposicao.getUrlInteiroTeor())) {
            documentos.put(proposicao.getUrlInteiroTeor(), DocumentoResumoDTO.builder()
                    .titulo("Inteiro teor")
                    .descricao("Texto integral disponibilizado pela fonte oficial")
                    .data(formatDate(proposicao.getDataApresentacao()))
                    .url(proposicao.getUrlInteiroTeor())
                    .origem(proposicao.getCasa() == CasaLegislativa.CAMARA ? "Câmara dos Deputados" : "Fonte oficial")
                    .build());
        }

        if (proposicao.getSilverSenadoId() != null) {
            silverSenadoDocumentoRepository.findBySenadoMateriaId(proposicao.getSilverSenadoId()).stream()
                    .filter(documento -> notBlank(documento.getUrlDocumento()))
                    .sorted(Comparator.comparing(this::parseDataDocumentoSenado,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(this::toDocumentoResumo)
                    .filter(java.util.Objects::nonNull)
                    .forEach(documento -> documentos.putIfAbsent(documento.getUrl(), documento));
        }

        return documentos.values().stream().limit(8).toList();
    }

    // ── Campos enriquecidos Silver Câmara ─────────────────────────────────────

    private String resolveEmentaDetalhadaCamara(Proposicao proposicao) {
        if (proposicao.getSilverCamaraId() == null)
            return null;
        var silverCamaraId = proposicao.getSilverCamaraId();
        return silverCamaraProposicaoRepository.findById(java.util.Objects.requireNonNull(silverCamaraId))
                .map(SilverCamaraProposicao::getEmentaDetalhada)
                .orElse(null);
    }

    private String resolveRegimeCamara(Proposicao proposicao) {
        if (proposicao.getSilverCamaraId() == null)
            return null;
        var silverCamaraId = proposicao.getSilverCamaraId();
        return silverCamaraProposicaoRepository.findById(java.util.Objects.requireNonNull(silverCamaraId))
                .map(s -> s.getStatusRegime() != null ? s.getStatusRegime() : s.getUltimoStatusRegime())
                .orElse(null);
    }

    private String resolveIndexacaoSenado(Proposicao proposicao) {
        if (proposicao.getSilverSenadoId() == null)
            return null;
        var silverSenadoId = proposicao.getSilverSenadoId();
        return silverSenadoMateriaRepository.findById(java.util.Objects.requireNonNull(silverSenadoId))
                .map(SilverSenadoMateria::getDetIndexacao)
                .orElse(null);
    }

    private String resolveOrgaoAtual(Proposicao proposicao) {
        if (proposicao.getCasa() != CasaLegislativa.CAMARA || proposicao.getSilverCamaraId() == null) {
            return null;
        }
        var silverCamaraId = proposicao.getSilverCamaraId();
        return silverCamaraProposicaoRepository.findById(java.util.Objects.requireNonNull(silverCamaraId))
                .map(s -> s.getStatusSiglaOrgao() != null ? s.getStatusSiglaOrgao() : s.getUltimoStatusSiglaOrgao())
                .orElse(null);
    }

    // ── Tramitações ───────────────────────────────────────────────────────────

    private List<TramitacaoDTO> buildTramitacoes(List<Tramitacao> tramitacoes) {
        return tramitacoes.stream()
                .map(t -> TramitacaoDTO.builder()
                        .dataFormatada(t.getDataHora() != null ? t.getDataHora().toLocalDate().format(BR_DATE) : null)
                        .siglaOrgao(t.getSiglaOrgao())
                        .descricaoOrgao(t.getDescricaoOrgao())
                        .descricao(t.getDescricaoTramitacao())
                        .situacao(t.getDescricaoSituacao())
                        .despacho(t.getDespacho())
                        .build())
                .toList();
    }

    private ProposicaoResumoDTO toProposicaoRelacionadaResumo(SilverCamaraProposicaoRelacionada relacionada) {
        if (relacionada.getRelacionadaId() == null) {
            return null;
        }
        String titulo = nvl(relacionada.getRelacionadaSiglaTipo(), "Proposição") + " "
                + nvl(relacionada.getRelacionadaNumero(), "s/n") + "/"
                + nvl(relacionada.getRelacionadaAno(), "s/ano");
        return ProposicaoResumoDTO.builder()
                .titulo(titulo)
                .ementa(relacionada.getRelacionadaEmenta())
                .situacao("Relacionada")
                .url("/stat-proposicoes/camara-" + relacionada.getRelacionadaId() + "/")
                .casa("camara")
                .casaLabel("Câmara dos Deputados")
                .build();
    }

    private VotacaoResumoDTO toVotacaoResumo(SilverCamaraVotacao votacao) {
        String resultado = Short.valueOf((short) 1).equals(votacao.getAprovacao()) ? "Aprovada"
                : Short.valueOf((short) 0).equals(votacao.getAprovacao()) ? "Rejeitada"
                        : "Resultado não informado";
        String placar = "%s sim · %s não · %s outros".formatted(
                zeroIfNull(votacao.getVotosSim()),
                zeroIfNull(votacao.getVotosNao()),
                zeroIfNull(votacao.getVotosOutros()));
        return VotacaoResumoDTO.builder()
                .data(votacao.getData() != null ? votacao.getData().format(BR_DATE) : null)
                .orgao(votacao.getSiglaOrgao())
                .resultado(resultado)
                .descricao(votacao.getDescricao())
                .placar(placar)
                .build();
    }

    private VotacaoResumoDTO toVotacaoResumo(SilverSenadoVotacao votacao) {
        return VotacaoResumoDTO.builder()
                .data(formatFlexibleDate(votacao.getDataSessao()))
                .orgao(nvl(votacao.getSiglaCasa(), votacao.getCodigoSessao()))
                .resultado(nvl(votacao.getDescricaoResultado(), votacao.getResultado(), "Resultado não informado"))
                .descricao(votacao.getDescricaoVotacao())
                .placar(buildPlacarSenado(votacao))
                .build();
    }

    private DocumentoResumoDTO toDocumentoResumo(SilverSenadoDocumento documento) {
        if (!notBlank(documento.getUrlDocumento())) {
            return null;
        }
        return DocumentoResumoDTO.builder()
                .titulo(nvl(documento.getTipoDocumento(), documento.getDescricaoTipoDocumento(), "Documento oficial"))
                .descricao(nvl(documento.getDescricaoDocumento(), documento.getAutorNome()))
                .data(formatFlexibleDate(documento.getDataDocumento()))
                .url(documento.getUrlDocumento())
                .origem("Senado Federal")
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String nvl(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer parseAnoRelacionada(SilverCamaraProposicaoRelacionada relacionada) {
        try {
            return relacionada.getRelacionadaAno() != null ? Integer.parseInt(relacionada.getRelacionadaAno()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Integer resolveCamaraId(Proposicao proposicao) {
        if (proposicao.getIdOrigem() == null) {
            return null;
        }
        try {
            return Integer.parseInt(proposicao.getIdOrigem());
        } catch (NumberFormatException e) {
            log.debug("Id de proposição da Câmara não numérico: {}", proposicao.getIdOrigem());
            return null;
        }
    }

    private int zeroIfNull(Integer value) {
        return value != null ? value : 0;
    }

    private String buildPlacarSenado(SilverSenadoVotacao votacao) {
        Integer sim = votacao.getTotalVotosSim();
        Integer nao = votacao.getTotalVotosNao();
        Integer abstencao = votacao.getTotalVotosAbstencao();
        if (sim == null && nao == null && abstencao == null) {
            return "Placar não informado";
        }
        return "%s sim · %s não · %s abstenções".formatted(
                zeroIfNull(sim),
                zeroIfNull(nao),
                zeroIfNull(abstencao));
    }

    private LocalDate parseDataSessaoSenado(SilverSenadoVotacao votacao) {
        return parseFlexibleDateToLocalDate(votacao.getDataSessao());
    }

    private LocalDate parseDataDocumentoSenado(SilverSenadoDocumento documento) {
        return parseFlexibleDateToLocalDate(documento.getDataDocumento());
    }

    private String formatFlexibleDate(String value) {
        LocalDate parsed = parseFlexibleDateToLocalDate(value);
        return parsed != null ? parsed.format(BR_DATE) : value;
    }

    private LocalDate parseFlexibleDateToLocalDate(String value) {
        if (!notBlank(value)) {
            return null;
        }
        String normalized = value.length() >= 10 ? value.substring(0, 10) : value;
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE, BR_DATE)) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveSituacaoTramitacao(Proposicao proposicao, String situacaoDescricao) {
        if (proposicao.isVirouLei() || "Encerrada".equalsIgnoreCase(situacaoDescricao)
                || "Arquivada".equalsIgnoreCase(situacaoDescricao)) {
            return "encerrada";
        }
        return "tramitando";
    }

    private String normalizeSituacaoDescricao(String situacao, CasaLegislativa casa) {
        if (!notBlank(situacao)) {
            return null;
        }
        if (casa == CasaLegislativa.SENADO) {
            if ("sim".equalsIgnoreCase(situacao)) {
                return "Em tramitação";
            }
            if ("não".equalsIgnoreCase(situacao) || "nao".equalsIgnoreCase(situacao)) {
                return "Encerrada";
            }
        }
        return situacao;
    }

    private String resolveDescricaoTipo(Proposicao proposicao) {
        if (proposicao.getTipo() != null && proposicao.getTipo().getDescricao() != null) {
            return proposicao.getTipo().getDescricao();
        }
        return proposicao.getSigla();
    }

    private String buildAutoriaResumo(List<AutorDTO> autores) {
        if (autores.isEmpty())
            return null;
        String primeiro = autores.get(0).getNome();
        if (autores.size() == 1)
            return primeiro;
        if (autores.size() == 2)
            return primeiro + " e " + autores.get(1).getNome();
        return primeiro + " e outros " + (autores.size() - 1);
    }

    private String buildSeoTitle(String sigla, Integer numero, Integer ano, String casaLabel) {
        return "%s %s/%s — %s | %s".formatted(sigla, numero, ano, casaLabel, SITE_NAME);
    }

    private String buildSeoDescription(String ementa, List<AutorDTO> autores) {
        if (ementa == null || ementa.isBlank())
            return SITE_NAME + " — Proposição legislativa";
        String base = ementa.length() > 150 ? ementa.substring(0, 147) + "..." : ementa;
        if (!autores.isEmpty()) {
            base += ". Autoria: " + autores.get(0).getNome();
        }
        return base;
    }

    private String buildSchemaLegislation(Proposicao p, String url, String sigla, String casaLabel,
            String datePublished, String dateModified) {
        try {
            var schema = Map.of(
                    "@context", "https://schema.org",
                    "@type", "Legislation",
                    "name", sigla + " " + p.getNumero() + "/" + p.getAno(),
                    "description", Optional.ofNullable(p.getEmenta()).orElse(""),
                    "url", url,
                    "legislationIdentifier", sigla + " " + p.getNumero() + "/" + p.getAno(),
                    "publisher", Map.of("@type", "GovernmentOrganization", "name", casaLabel),
                    "datePublished", Optional.ofNullable(datePublished).orElse(""),
                    "dateModified", Optional.ofNullable(dateModified).orElse(""));
            return escapeJsonForHtml(objectMapper.writeValueAsString(schema));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar schema Legislation para {}: {}", url, e.getMessage());
            return "{\"@context\":\"https://schema.org\",\"@type\":\"Legislation\"}";
        }
    }

    private String buildSchemaBreadcrumb(String url, String sigla, Integer numero, Integer ano) {
        try {
            var items = List.of(
                    Map.of("@type", "ListItem", "position", 1, "name", "Início", "item", BASE_URL),
                    Map.of("@type", "ListItem", "position", 2, "name", "Proposições", "item",
                            BASE_URL + "/stat-proposicoes"),
                    Map.of("@type", "ListItem", "position", 3, "name", sigla + " " + numero + "/" + ano, "item", url));
            var schema = Map.of(
                    "@context", "https://schema.org",
                    "@type", "BreadcrumbList",
                    "itemListElement", items);
            return escapeJsonForHtml(objectMapper.writeValueAsString(schema));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar BreadcrumbList para {}: {}", url, e.getMessage());
            return "{\"@context\":\"https://schema.org\",\"@type\":\"BreadcrumbList\"}";
        }
    }

    // ── JSON embutido para hidratação Vue ────────────────────────────────────

    /**
     * Serializa os dados da proposição como JSON compatível com a interface
     * {@code ProposicaoCompleta} do frontend Nuxt, para ser embutido no HTML
     * estático gerado e lido pelo componente Vue na hidratação inicial.
     */
    @SuppressWarnings("java:S107") // muitos parâmetros justificados — evita repetir consultas
    private String buildProposicaoJson(
            String casa, String idOriginal, String siglaTipo, String descricaoTipo,
            Proposicao p, List<AutorDTO> autores, List<Tramitacao> tramitacoes,
            List<String> temas, String ementaDetalhada, String orgaoAtual, String regime,
            String autoriaResumo, String situacaoDescricao, String situacaoTramitacao,
            String dataApresentacaoIso, String dataAtualizacaoIso, List<String> keywords) {
        try {
            var autoresJson = autores.stream().map(a -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("nome", a.getNome());
                m.put("tipo", a.getTipo() != null ? a.getTipo() : "");
                m.put("proponente", a.isProponente());
                if (a.getCasa() != null)
                    m.put("casa", a.getCasa());
                if (a.getIdOriginal() != null)
                    m.put("idOriginal", a.getIdOriginal());
                return m;
            }).toList();

            var tramitacoesJson = tramitacoes.stream().map(t -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("dataHora", t.getDataHora() != null ? t.getDataHora().format(ISO_DT) : "");
                m.put("siglaOrgao", t.getSiglaOrgao() != null ? t.getSiglaOrgao() : "");
                m.put("descricao", t.getDescricaoTramitacao() != null ? t.getDescricaoTramitacao() : "");
                m.put("situacao", t.getDescricaoSituacao() != null ? t.getDescricaoSituacao() : "");
                m.put("despacho", t.getDespacho() != null ? t.getDespacho() : "");
                return m;
            }).toList();

            var json = new LinkedHashMap<String, Object>();
            json.put("id", casa + ":" + idOriginal);
            json.put("idOriginal", idOriginal);
            json.put("casa", casa);
            json.put("siglaTipo", siglaTipo);
            json.put("descricaoTipo", descricaoTipo != null ? descricaoTipo : siglaTipo);
            json.put("numero", p.getNumero());
            json.put("ano", p.getAno());
            json.put("ementa", p.getEmenta() != null ? p.getEmenta() : "");
            json.put("dataApresentacao", dataApresentacaoIso != null ? dataApresentacaoIso : "");
            json.put("autoria", autoriaResumo != null ? autoriaResumo : "");
            json.put("situacaoTramitacao", situacaoTramitacao);
            json.put("situacaoDescricao", situacaoDescricao != null ? situacaoDescricao : "");
            if (dataAtualizacaoIso != null)
                json.put("dataUltimaAtualizacao", dataAtualizacaoIso);
            json.put("ementaDetalhada", ementaDetalhada != null ? ementaDetalhada : "");
            json.put("keywords", keywords);
            json.put("urlInteiroTeor", p.getUrlInteiroTeor());
            json.put("orgaoAtual", orgaoAtual);
            json.put("regime", regime);
            json.put("temas", temas);
            json.put("autores", autoresJson);
            json.put("tramitacoes", tramitacoesJson);
            return escapeJsonForHtml(objectMapper.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar JSON embutido para {}-{}: {}", casa, idOriginal, e.getMessage());
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(BR_DATE) : null;
    }

    private List<String> parseKeywords(String kw) {
        if (kw == null || kw.isBlank())
            return List.of();
        var result = new ArrayList<String>();
        for (String s : kw.split("[,;]+\\s*")) {
            String t = s.trim();
            if (!t.isBlank())
                result.add(t);
        }
        return result;
    }

    private String escapeJsonForHtml(String json) {
        if (json == null)
            return null;
        return json.replace("<", "\\u003c").replace(">", "\\u003e");
    }
}
