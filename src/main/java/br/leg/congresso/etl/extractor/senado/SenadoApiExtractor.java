package br.leg.congresso.etl.extractor.senado;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAutoriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDocumentoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoEmendaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPrazoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRefDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRelatoriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoTextoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoVotacaoDTO;
import br.leg.congresso.etl.extractor.senado.mapper.SenadoMateriaMapper;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai matérias legislativas do Senado Federal via API REST.
 * Usa rate limiting adaptativo e Resilience4j para fault tolerance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SenadoApiExtractor {

    private final @Qualifier("senadoWebClient") WebClient senadoWebClient;
    private final SenadoMateriaMapper mapper;
    private final AdaptiveRateLimiter rateLimiter;

    @Value("${etl.senado.tipos-aceitos:PL,PLS,PLC,MPV,PEC,PDL,PRN,PDS}")
    private String tiposAceitos;

    @Value("${etl.senado.ano-inicio:1988}")
    private int anoInicio;

    @Value("${etl.senado.enrich-url-texto:false}")
    private boolean enrichUrlTexto;

    private static final String PESQUISA_PATH = "/dadosabertos/materia/pesquisa/lista.json";
    private static final DateTimeFormatter SENADO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Extrai todas as matérias para o intervalo de anos especificado (carga total).
     */
    public List<Proposicao> extractByYearRange(int anoInicioParam, int anoFim) {
        log.info("Extração Senado: anos {} a {}", anoInicioParam, anoFim);
        List<Proposicao> all = new ArrayList<>();

        for (int ano = anoInicioParam; ano <= anoFim; ano++) {
            for (String sigla : tiposAceitos.split(",")) {
                sigla = sigla.trim();
                if (sigla.isEmpty())
                    continue;
                try {
                    List<Proposicao> found = fetchByAnoTipo(ano, sigla);
                    all.addAll(found);
                    rateLimiter.adjustOnSuccess();
                    throttle();
                } catch (Exception e) {
                    log.error("Erro ao buscar Senado: ano={}, tipo={}: {}", ano, sigla, e.getMessage());
                }
            }
        }

        log.info("Extração Senado concluída: {} matérias encontradas", all.size());
        return all;
    }

    /**
     * Extrai matérias brutas (DTOs da fonte) para carga Silver — sem mapeamento
     * para domínio.
     * Preserva todos os campos originais da API para passthrough fiel à fonte.
     */
    public List<SenadoMateriaDTO.Materia> extractRawByYearRange(int anoInicioParam, int anoFim) {
        log.info("[Silver] Extração raw Senado: anos {} a {}", anoInicioParam, anoFim);
        List<SenadoMateriaDTO.Materia> all = new ArrayList<>();

        for (int ano = anoInicioParam; ano <= anoFim; ano++) {
            for (String sigla : tiposAceitos.split(",")) {
                sigla = sigla.trim();
                if (sigla.isEmpty())
                    continue;
                try {
                    List<SenadoMateriaDTO.Materia> found = fetchRawByAnoTipo(ano, sigla);
                    all.addAll(found);
                    rateLimiter.adjustOnSuccess();
                    throttle();
                } catch (Exception e) {
                    log.error("[Silver] Erro ao buscar Senado raw: ano={}, tipo={}: {}", ano, sigla, e.getMessage());
                }
            }
        }

        log.info("[Silver] Extração raw Senado concluída: {} matérias encontradas", all.size());
        return all;
    }

    /**
     * Extrai matérias por intervalo de datas (carga incremental).
     */
    public List<Proposicao> extractByDateRange(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Extração incremental Senado: {} a {}", dataInicio, dataFim);
        String dataInicioStr = dataInicio.format(SENADO_DATE_FMT);
        String dataFimStr = dataFim.format(SENADO_DATE_FMT);

        List<Proposicao> all = new ArrayList<>();
        for (String sigla : tiposAceitos.split(",")) {
            sigla = sigla.trim();
            if (sigla.isEmpty())
                continue;
            try {
                List<Proposicao> found = fetchByDataTipo(dataInicioStr, dataFimStr, sigla);
                all.addAll(found);
                rateLimiter.adjustOnSuccess();
                throttle();
            } catch (Exception e) {
                log.error("Erro ao buscar Senado incremental: tipo={}: {}", sigla, e.getMessage());
            }
        }

        log.info("Extração incremental Senado: {} matérias encontradas", all.size());
        return all;
    }

    /**
     * Extrai matérias brutas (DTOs) por intervalo de datas (incremental Silver).
     */
    public List<SenadoMateriaDTO.Materia> extractRawByDateRange(LocalDate dataInicio, LocalDate dataFim) {
        log.info("[Silver] Extração incremental raw Senado: {} a {}", dataInicio, dataFim);
        String dataInicioStr = dataInicio.format(SENADO_DATE_FMT);
        String dataFimStr = dataFim.format(SENADO_DATE_FMT);

        List<SenadoMateriaDTO.Materia> all = new ArrayList<>();
        for (String sigla : tiposAceitos.split(",")) {
            sigla = sigla.trim();
            if (sigla.isEmpty())
                continue;
            try {
                List<SenadoMateriaDTO.Materia> found = fetchRawByDataTipo(dataInicioStr, dataFimStr, sigla);
                all.addAll(found);
                rateLimiter.adjustOnSuccess();
                throttle();
            } catch (Exception e) {
                log.error("[Silver] Erro ao buscar Senado raw incremental: tipo={}: {}", sigla, e.getMessage());
            }
        }

        log.info("[Silver] Extração incremental raw Senado: {} matérias encontradas", all.size());
        return all;
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    private List<SenadoMateriaDTO.Materia> fetchRawByAnoTipo(int ano, String siglaTipo) {
        try {
            SenadoMateriaDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PESQUISA_PATH)
                            .queryParam("sigla", siglaTipo)
                            .queryParam("ano", ano)
                            .build())
                    .retrieve()
                    .bodyToMono(SenadoMateriaDTO.class)
                    .block();

            return extractMateriasRaw(response);
        } catch (WebClientResponseException.TooManyRequests e) {
            rateLimiter.adjustOnTooManyRequests();
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            throw e;
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    private List<Proposicao> fetchByAnoTipo(int ano, String siglaTipo) {
        try {
            SenadoMateriaDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PESQUISA_PATH)
                            .queryParam("sigla", siglaTipo)
                            .queryParam("ano", ano)
                            .build())
                    .retrieve()
                    .bodyToMono(SenadoMateriaDTO.class)
                    .block();

            return extractMaterias(response);
        } catch (WebClientResponseException.TooManyRequests e) {
            rateLimiter.adjustOnTooManyRequests();
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            throw e;
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    private List<Proposicao> fetchByDataTipo(String dataInicio, String dataFim, String siglaTipo) {
        try {
            SenadoMateriaDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PESQUISA_PATH)
                            .queryParam("sigla", siglaTipo)
                            .queryParam("dataInicio", dataInicio)
                            .queryParam("dataFim", dataFim)
                            .build())
                    .retrieve()
                    .bodyToMono(SenadoMateriaDTO.class)
                    .block();

            return extractMaterias(response);
        } catch (WebClientResponseException.TooManyRequests e) {
            rateLimiter.adjustOnTooManyRequests();
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            throw e;
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    private List<SenadoMateriaDTO.Materia> fetchRawByDataTipo(String dataInicio, String dataFim, String siglaTipo) {
        try {
            SenadoMateriaDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PESQUISA_PATH)
                            .queryParam("sigla", siglaTipo)
                            .queryParam("dataInicio", dataInicio)
                            .queryParam("dataFim", dataFim)
                            .build())
                    .retrieve()
                    .bodyToMono(SenadoMateriaDTO.class)
                    .block();

            return extractMateriasRaw(response);
        } catch (WebClientResponseException.TooManyRequests e) {
            rateLimiter.adjustOnTooManyRequests();
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            throw e;
        }
    }

    /**
     * Extrai a lista de tramitações de uma matéria.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<Tramitacao> fetchMovimentacoes(String codigoMateria) {
        try {
            SenadoMovimentacaoDTO response = senadoWebClient.get()
                    .uri("/dadosabertos/materia/{id}/movimentacoes.json", codigoMateria)
                    .retrieve()
                    .bodyToMono(SenadoMovimentacaoDTO.class)
                    .block(Duration.ofSeconds(12));

            if (response == null || response.getMovimentacoesResponse() == null) {
                return Collections.emptyList();
            }

            var materia = response.getMovimentacoesResponse().getMateria();
            if (materia == null || materia.getMovimentacoes() == null
                    || materia.getMovimentacoes().getMovimentacao() == null) {
                return Collections.emptyList();
            }

            return materia.getMovimentacoes().getMovimentacao().stream()
                    .map(mapper::movimentacaoToTramitacao)
                    .toList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("Movimentações Senado não encontradas para código={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Não foi possível buscar movimentações do Senado para código={}: {}", codigoMateria,
                    e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SenadoMateriaDTO.Materia> extractMateriasRaw(SenadoMateriaDTO response) {
        if (response == null || response.getPesquisaBasicaResponse() == null) {
            return Collections.emptyList();
        }
        var wrapper = response.getPesquisaBasicaResponse().getMaterias();
        if (wrapper == null || wrapper.getMateria() == null) {
            return Collections.emptyList();
        }
        return wrapper.getMateria().stream()
                .filter(m -> TipoProposicaoNormalizer.isProposicaoAceita(m.getSiglaSubtipoMateria()))
                .toList();
    }

    private List<Proposicao> extractMaterias(SenadoMateriaDTO response) {
        if (response == null || response.getPesquisaBasicaResponse() == null) {
            return Collections.emptyList();
        }
        var wrapper = response.getPesquisaBasicaResponse().getMaterias();
        if (wrapper == null || wrapper.getMateria() == null) {
            return Collections.emptyList();
        }

        return wrapper.getMateria().stream()
                .filter(m -> TipoProposicaoNormalizer.isProposicaoAceita(m.getSiglaSubtipoMateria()))
                .map(m -> {
                    Proposicao proposicao = mapper.materiaToProposicao(m);
                    if (enrichUrlTexto && proposicao.getIdOrigem() != null && !proposicao.getIdOrigem().isBlank()) {
                        String urlTexto = fetchUrlTextoMateria(proposicao.getIdOrigem());
                        if (urlTexto != null && !urlTexto.isBlank()) {
                            proposicao.setUrlInteiroTeor(urlTexto);
                        }
                    }
                    return proposicao;
                })
                .toList();
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    private String fetchUrlTextoMateria(String codigoMateria) {
        try {
            SenadoTextoMateriaDTO response = senadoWebClient.get()
                    .uri("/dadosabertos/materia/textos/{codigo}.json", codigoMateria)
                    .retrieve()
                    .bodyToMono(SenadoTextoMateriaDTO.class)
                    .block();

            if (response == null || response.getTextoMateria() == null
                    || response.getTextoMateria().getMateria() == null
                    || response.getTextoMateria().getMateria().getTextos() == null
                    || response.getTextoMateria().getMateria().getTextos().getTexto() == null) {
                return null;
            }

            return response.getTextoMateria().getMateria().getTextos().getTexto().stream()
                    .map(SenadoTextoMateriaDTO.Texto::getUrlTexto)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Falha ao buscar UrlTexto no Senado para codigo={}: {}", codigoMateria, e.getMessage());
            return null;
        }
    }

    /**
     * Busca as emendas de uma matéria.
     * GET /dadosabertos/processo/emenda?codigoMateria={codigo}
     *
     * @return lista de emendas ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoEmendaDTO> fetchEmendas(String codigoMateria) {
        try {
            List<SenadoEmendaDTO> response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dadosabertos/processo/emenda")
                            .queryParam("codigoMateria", codigoMateria)
                            .build())
                    .retrieve()
                    .bodyToFlux(SenadoEmendaDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Emendas Senado não encontradas: codigoMateria={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar emendas Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca os documentos de uma matéria.
     * GET /dadosabertos/processo/documento?codigoMateria={codigo}
     *
     * @return lista de documentos ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoDocumentoDTO> fetchDocumentos(String codigoMateria) {
        try {
            List<SenadoDocumentoDTO> response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dadosabertos/processo/documento")
                            .queryParam("codigoMateria", codigoMateria)
                            .build())
                    .retrieve()
                    .bodyToFlux(SenadoDocumentoDTO.class)
                    .collectList()
                    .block(Duration.ofSeconds(12));
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Documentos Senado não encontrados: codigoMateria={}", codigoMateria);
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            log.warn("[Silver] Falha ao buscar documentos Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar documentos Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca os prazos de uma matéria.
     * GET /dadosabertos/processo/prazo?codigoMateria={codigo}
     *
     * @return lista de prazos ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoPrazoDTO> fetchPrazos(String codigoMateria) {
        try {
            List<SenadoPrazoDTO> response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dadosabertos/processo/prazo")
                            .queryParam("codigoMateria", codigoMateria)
                            .build())
                    .retrieve()
                    .bodyToFlux(SenadoPrazoDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Prazos Senado não encontrados: codigoMateria={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar prazos Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca as votações de uma matéria.
     * GET /dadosabertos/votacao?codigoMateria={codigo}
     *
     * @return lista de votações ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoVotacaoDTO> fetchVotacoes(String codigoMateria) {
        try {
            List<SenadoVotacaoDTO> response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dadosabertos/votacao")
                            .queryParam("codigoMateria", codigoMateria)
                            .build())
                    .retrieve()
                    .bodyToFlux(SenadoVotacaoDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Votações Senado não encontradas: codigoMateria={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar votações Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de tipos de situação de processos (dados de referência
     * estática).
     * GET /dadosabertos/processo/tipos-situacao
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchTiposSituacao() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/tipos-situacao")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] tipos-situacao Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar tipos-situacao Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de tipos de decisão de processos (dados de referência
     * estática).
     * GET /dadosabertos/processo/tipos-decisao
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchTiposDecisao() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/tipos-decisao")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] tipos-decisao Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar tipos-decisao Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de tipos de autor de processos (dados de referência estática).
     * GET /dadosabertos/processo/tipos-autor
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchTiposAutor() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/tipos-autor")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] tipos-autor Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar tipos-autor Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de siglas de processos (dados de referência estática).
     * GET /dadosabertos/processo/siglas
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchSiglas() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/siglas")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] siglas Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar siglas Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de classes de processos (dados de referência estática).
     * GET /dadosabertos/processo/classes
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchClasses() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/classes")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] classes Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar classes Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de assuntos de processos (dados de referência estática).
     * GET /dadosabertos/processo/assuntos
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRefDTO> fetchAssuntos() {
        try {
            List<SenadoRefDTO> response = senadoWebClient.get()
                    .uri("/dadosabertos/processo/assuntos")
                    .retrieve()
                    .bodyToFlux(SenadoRefDTO.class)
                    .collectList()
                    .block();
            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] assuntos Senado não encontrado");
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar assuntos Senado: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void throttle() {
        try {
            Thread.sleep(rateLimiter.getDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Busca os dados de detalhe de uma matéria pelo código.
     * Retorna null em caso de erro para permitir fallback gracioso.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public SenadoDetalheDTO fetchRawDetalhe(String codigo) {
        try {
            return senadoWebClient.get()
                    .uri("/dadosabertos/materia/{codigo}.json", codigo)
                    .retrieve()
                    .bodyToMono(SenadoDetalheDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Detalhe Senado não encontrado para codigo={}", codigo);
            return null;
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar detalhe Senado codigo={}: {}", codigo, e.getMessage());
            return null;
        }
    }

    /**
     * Busca as movimentações brutas de uma matéria (sem mapeamento para domínio).
     * Para uso no enriquecimento Silver.
     *
     * @return lista de movimentações ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoMovimentacaoDTO.Movimentacao> fetchRawMovimentacoes(String codigoMateria) {
        try {
            SenadoMovimentacaoDTO response = senadoWebClient.get()
                    .uri("/dadosabertos/materia/{id}/movimentacoes.json", codigoMateria)
                    .retrieve()
                    .bodyToMono(SenadoMovimentacaoDTO.class)
                    .block(Duration.ofSeconds(12));

            if (response == null || response.getMovimentacoesResponse() == null) {
                return Collections.emptyList();
            }
            var materia = response.getMovimentacoesResponse().getMateria();
            if (materia == null || materia.getMovimentacoes() == null
                    || materia.getMovimentacoes().getMovimentacao() == null) {
                return Collections.emptyList();
            }
            return materia.getMovimentacoes().getMovimentacao();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Movimentações Senado não encontradas: codigo={}", codigoMateria);
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                rateLimiter.adjustOn5xx();
            }
            log.warn("[Silver] Falha ao buscar movimentações Senado codigo={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar movimentações Senado codigo={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca os autores de uma matéria (endpoint legado, tolerante a falhas).
     * GET /dadosabertos/materia/autoria/{codigo}.json
     *
     * @return lista de autores ou lista vazia em caso de erro / endpoint
     *         indisponível
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoAutoriaDTO.Autor> fetchRawAutoria(String codigoMateria) {
        try {
            SenadoAutoriaDTO response = senadoWebClient.get()
                    .uri("/dadosabertos/materia/autoria/{codigo}.json", codigoMateria)
                    .retrieve()
                    .bodyToMono(SenadoAutoriaDTO.class)
                    .block();

            if (response == null || response.getAutoriaBasicaMateria() == null) {
                return Collections.emptyList();
            }
            var materiaDto = response.getAutoriaBasicaMateria().getMateria();
            if (materiaDto == null || materiaDto.getAutores() == null
                    || materiaDto.getAutores().getAutor() == null) {
                return Collections.emptyList();
            }
            return materiaDto.getAutores().getAutor();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Autoria Senado não encontrada: codigo={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar autoria Senado codigo={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca as relatorias de um processo legislativo por código de matéria.
     * GET /dadosabertos/processo/relatoria?codigoMateria={codigo}
     *
     * @return lista de relatorias ou lista vazia em caso de erro
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoRelatoriaDTO> fetchRelatorias(String codigoMateria) {
        try {
            List<SenadoRelatoriaDTO> response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dadosabertos/processo/relatoria")
                            .queryParam("codigoMateria", codigoMateria)
                            .build())
                    .retrieve()
                    .bodyToFlux(SenadoRelatoriaDTO.class)
                    .collectList()
                    .block();

            return response != null ? response : Collections.emptyList();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Silver] Relatorias Senado não encontradas: codigoMateria={}", codigoMateria);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar relatorias Senado codigoMateria={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }
}
