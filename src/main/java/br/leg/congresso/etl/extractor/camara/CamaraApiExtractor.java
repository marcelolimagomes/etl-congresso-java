package br.leg.congresso.etl.extractor.camara;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extrai proposições da Câmara via API REST para carga incremental.
 * Utiliza paginação e filtra pelos tipos de proposição aceitos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraApiExtractor {

    private final @Qualifier("camaraWebClient") WebClient camaraWebClient;
    private final CamaraProposicaoMapper mapper;

    @Value("${etl.camara.tipos-aceitos:PL,PLP,MPV,PEC,PDL,PR,PDS}")
    private String tiposAceitos;

    private static final DateTimeFormatter API_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int PAGE_SIZE = 100;

    /**
     * Busca proposições atualizadas entre as datas informadas.
     */
    public List<Proposicao> extractByDateRange(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Extração incremental Câmara: {} a {}", dataInicio, dataFim);

        List<Proposicao> all = new ArrayList<>();
        for (String sigla : tiposAceitos.split(",")) {
            sigla = sigla.trim();
            if (sigla.isEmpty()) continue;
            log.debug("Buscando tipo {} entre {} e {}", sigla, dataInicio, dataFim);
            List<Proposicao> paginados = fetchAllPages(sigla, dataInicio, dataFim);
            all.addAll(paginados);
        }

        log.info("Extração incremental Câmara concluída: {} proposições encontradas", all.size());
        return all;
    }

    /**
     * Busca proposições brutas (DTOs) atualizadas entre as datas informadas.
     * Usado na carga incremental Silver (passthrough da fonte).
     */
    public List<CamaraProposicaoDTO> extractRawByDateRange(LocalDate dataInicio, LocalDate dataFim) {
        log.info("[Silver] Extração incremental raw Câmara: {} a {}", dataInicio, dataFim);

        List<CamaraProposicaoDTO> all = new ArrayList<>();
        for (String sigla : tiposAceitos.split(",")) {
            sigla = sigla.trim();
            if (sigla.isEmpty()) continue;
            List<CamaraProposicaoDTO> paginados = fetchAllPagesRaw(sigla, dataInicio, dataFim);
            all.addAll(paginados);
        }

        log.info("[Silver] Extração incremental raw Câmara concluída: {} proposições encontradas", all.size());
        return all;
    }

    /**
     * Busca todas as páginas para um tipo de proposição e intervalo de datas.
     */
    private List<Proposicao> fetchAllPages(String siglaTipo, LocalDate dataInicio, LocalDate dataFim) {
        List<Proposicao> result = new ArrayList<>();
        int pagina = 1;

        while (true) {
            CamaraProposicaoDTO.ListResponse response = fetchPage(siglaTipo, dataInicio, dataFim, pagina);
            if (response == null || response.getDados() == null || response.getDados().isEmpty()) {
                break;
            }

            for (CamaraProposicaoDTO dto : response.getDados()) {
                if (TipoProposicaoNormalizer.isProposicaoAceita(dto.getSiglaTipo())) {
                    enrichComDetalheSeNecessario(dto);
                    result.add(mapper.apiDtoToProposicao(dto));
                }
            }

            // Verifica se há próxima página
            boolean hasNext = response.getLinks() != null && response.getLinks().stream()
                    .anyMatch(l -> "next".equals(l.getRel()) && l.getHref() != null && !l.getHref().isEmpty());

            if (!hasNext || response.getDados().size() < PAGE_SIZE) break;
            pagina++;
        }

        return result;
    }

    private List<CamaraProposicaoDTO> fetchAllPagesRaw(String siglaTipo, LocalDate dataInicio, LocalDate dataFim) {
        List<CamaraProposicaoDTO> result = new ArrayList<>();
        int pagina = 1;

        while (true) {
            CamaraProposicaoDTO.ListResponse response = fetchPage(siglaTipo, dataInicio, dataFim, pagina);
            if (response == null || response.getDados() == null || response.getDados().isEmpty()) {
                break;
            }

            for (CamaraProposicaoDTO dto : response.getDados()) {
                if (TipoProposicaoNormalizer.isProposicaoAceita(dto.getSiglaTipo())) {
                    result.add(dto);
                }
            }

            boolean hasNext = response.getLinks() != null && response.getLinks().stream()
                    .anyMatch(l -> "next".equals(l.getRel()) && l.getHref() != null && !l.getHref().isEmpty());

            if (!hasNext || response.getDados().size() < PAGE_SIZE) break;
            pagina++;
        }

        return result;
    }

    private void enrichComDetalheSeNecessario(CamaraProposicaoDTO dto) {
        if ((dto.getUrlInteiroTeor() == null || dto.getUrlInteiroTeor().isBlank())
                || (dto.getKeywords() == null || dto.getKeywords().isBlank())) {
            CamaraProposicaoDTO detalhe = fetchDetalhe(dto.getId());
            if (detalhe != null) {
                if ((dto.getUrlInteiroTeor() == null || dto.getUrlInteiroTeor().isBlank())
                        && detalhe.getUrlInteiroTeor() != null && !detalhe.getUrlInteiroTeor().isBlank()) {
                    dto.setUrlInteiroTeor(detalhe.getUrlInteiroTeor());
                }
                if ((dto.getKeywords() == null || dto.getKeywords().isBlank())
                        && detalhe.getKeywords() != null && !detalhe.getKeywords().isBlank()) {
                    dto.setKeywords(detalhe.getKeywords());
                }
                if ((dto.getUltimoStatus() == null) && detalhe.getUltimoStatus() != null) {
                    dto.setUltimoStatus(detalhe.getUltimoStatus());
                }
            }
        }
    }

    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    private CamaraProposicaoDTO.ListResponse fetchPage(String siglaTipo, LocalDate dataInicio,
                                                        LocalDate dataFim, int pagina) {
        try {
            return camaraWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/proposicoes")
                            .queryParam("siglaTipo", siglaTipo)
                            .queryParam("dataInicio", dataInicio.format(API_DATE_FMT))
                            .queryParam("dataFim", dataFim.format(API_DATE_FMT))
                            .queryParam("itens", PAGE_SIZE)
                            .queryParam("pagina", pagina)
                            .queryParam("ordem", "ASC")
                            .queryParam("ordenarPor", "id")
                            .build())
                    .retrieve()
                    .bodyToMono(CamaraProposicaoDTO.ListResponse.class)
                    .block();
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("Rate limit Câmara API. Tipo={}, página={}", siglaTipo, pagina);
            throw e;
        } catch (Exception e) {
            log.error("Erro ao buscar proposições Câmara. Tipo={}, página={}: {}", siglaTipo, pagina, e.getMessage());
            throw e;
        }
    }

    /**
     * Busca as tramitações brutas (DTOs da fonte) para carga Silver.
     * Preserva todos os campos originais da API para passthrough fiel.
     *
     * @param camaraId ID da proposição na API da Câmara
     * @return lista de DTOs de tramitação; lista vazia em caso de erro
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<CamaraTramitacaoDTO> fetchTramitacoesRaw(String camaraId) {
        try {
            CamaraTramitacaoDTO.ListResponse response = camaraWebClient.get()
                    .uri("/api/v2/proposicoes/{id}/tramitacoes", camaraId)
                    .retrieve()
                    .bodyToMono(CamaraTramitacaoDTO.ListResponse.class)
                    .block();

            if (response == null || response.getDados() == null) {
                return Collections.emptyList();
            }

            return response.getDados();
        } catch (Exception e) {
            log.warn("Não foi possível buscar tramitações raw para camaraId={}: {}", camaraId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca as tramitações de uma proposição pelo seu ID de origem.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<Tramitacao> fetchTramitacoes(String idOrigem) {
        try {
            CamaraTramitacaoDTO.ListResponse response = camaraWebClient.get()
                    .uri("/api/v2/proposicoes/{id}/tramitacoes", idOrigem)
                    .retrieve()
                    .bodyToMono(CamaraTramitacaoDTO.ListResponse.class)
                    .block();

            if (response == null || response.getDados() == null) {
                return Collections.emptyList();
            }

            return response.getDados().stream()
                    .map(mapper::tramitacaoDtoToTramitacao)
                    .toList();
        } catch (Exception e) {
            log.warn("Não foi possível buscar tramitações para idOrigem={}: {}", idOrigem, e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    private CamaraProposicaoDTO fetchDetalhe(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return camaraWebClient.get()
                    .uri("/api/v2/proposicoes/{id}", id)
                    .retrieve()
                    .bodyToMono(CamaraProposicaoDTO.class)
                    .block();
        } catch (Exception e) {
            log.debug("Falha ao enriquecer detalhe da proposição Câmara id={}: {}", id, e.getMessage());
            return null;
        }
    }
}
