package br.leg.congresso.etl.extractor.senado;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoTextoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.mapper.SenadoMateriaMapper;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                if (sigla.isEmpty()) continue;
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
     * Extrai matérias brutas (DTOs da fonte) para carga Silver — sem mapeamento para domínio.
     * Preserva todos os campos originais da API para passthrough fiel à fonte.
     */
    public List<SenadoMateriaDTO.Materia> extractRawByYearRange(int anoInicioParam, int anoFim) {
        log.info("[Silver] Extração raw Senado: anos {} a {}", anoInicioParam, anoFim);
        List<SenadoMateriaDTO.Materia> all = new ArrayList<>();

        for (int ano = anoInicioParam; ano <= anoFim; ano++) {
            for (String sigla : tiposAceitos.split(",")) {
                sigla = sigla.trim();
                if (sigla.isEmpty()) continue;
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
            if (sigla.isEmpty()) continue;
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
            if (sigla.isEmpty()) continue;
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
                    .block();

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
        } catch (Exception e) {
            log.warn("Não foi possível buscar movimentações do Senado para código={}: {}", codigoMateria, e.getMessage());
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
                    .block();

            if (response == null || response.getMovimentacoesResponse() == null) {
                return Collections.emptyList();
            }
            var materia = response.getMovimentacoesResponse().getMateria();
            if (materia == null || materia.getMovimentacoes() == null
                    || materia.getMovimentacoes().getMovimentacao() == null) {
                return Collections.emptyList();
            }
            return materia.getMovimentacoes().getMovimentacao();
        } catch (Exception e) {
            log.warn("[Silver] Falha ao buscar movimentações Senado codigo={}: {}", codigoMateria, e.getMessage());
            return Collections.emptyList();
        }
    }
}
