package br.leg.congresso.etl.extractor.camara;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoDetalheDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoDiscursoDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoEventoDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoHistoricoDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoMandatoExternoDTO;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoDiscursoLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoEventoLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoHistoricoLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoLoader;
import br.leg.congresso.etl.loader.silver.SilverCamaraDeputadoMandatoExternoLoader;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai sub-recursos de deputados da Câmara via API REST e persiste na camada
 * Silver.
 *
 * Endpoints cobertos:
 * <ul>
 * <li>GET /api/v2/deputados/{id}/discursos →
 * silver.camara_deputado_discurso</li>
 * <li>GET /api/v2/deputados/{id}/eventos → silver.camara_deputado_evento</li>
 * <li>GET /api/v2/deputados/{id}/historico →
 * silver.camara_deputado_historico</li>
 * <li>GET /api/v2/deputados/{id}/mandatosExternos →
 * silver.camara_deputado_mandato_externo</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraDeputadoApiExtractor {

    private final @Qualifier("camaraWebClient") WebClient camaraWebClient;

    private final SilverCamaraDeputadoRepository deputadoRepository;
    private final SilverCamaraDeputadoLoader deputadoLoader;
    private final SilverCamaraDeputadoDiscursoLoader discursoLoader;
    private final SilverCamaraDeputadoEventoLoader eventoLoader;
    private final SilverCamaraDeputadoHistoricoLoader historicoLoader;
    private final SilverCamaraDeputadoMandatoExternoLoader mandatoExternoLoader;

    private static final int PAGE_SIZE = 100;

    /**
     * Itera sobre todos os deputados Silver e extrai/persiste os 4 sub-recursos de
     * cada um.
     *
     * @param jobControl job ETL corrente — usado para rastreabilidade
     * @return total de registros inseridos (soma de todos os sub-recursos)
     */
    public int extrairEPersistirTodos(EtlJobControl jobControl) {
        List<String> deputadoIds = deputadoRepository.findAll()
                .stream()
                .map(d -> d.getCamaraId())
                .filter(id -> id != null && !id.isBlank())
                .toList();

        log.info("[CamaraDeputadoApiExtractor] Iniciando extração API para {} deputados", deputadoIds.size());

        int totalInseridos = 0;
        for (String deputadoId : deputadoIds) {
            totalInseridos += extrairEPersistirPorDeputado(deputadoId, jobControl);
        }

        log.info("[CamaraDeputadoApiExtractor] Extração concluída: {} registros inseridos no total",
                totalInseridos);
        return totalInseridos;
    }

    /**
     * Extrai e persiste os 4 sub-recursos para um único deputado.
     *
     * @param deputadoId ID do deputado na API da Câmara
     * @param jobControl job ETL corrente
     * @return total de registros inseridos para este deputado
     */
    public int extrairEPersistirPorDeputado(String deputadoId, EtlJobControl jobControl) {
        int inseridos = 0;

        inseridos += discursoLoader.carregar(
                deputadoId, fetchDiscursos(deputadoId), jobControl.getId());
        inseridos += eventoLoader.carregar(
                deputadoId, fetchEventos(deputadoId), jobControl.getId());
        inseridos += historicoLoader.carregar(
                deputadoId, fetchHistorico(deputadoId), jobControl.getId());
        inseridos += mandatoExternoLoader.carregar(
                deputadoId, fetchMandatosExternos(deputadoId), jobControl.getId());

        return inseridos;
    }

    // ── Métodos de fetch individuais ─────────────────────────────────────────

    /**
     * Busca todos os discursos de um deputado com paginação automática.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<CamaraDeputadoDiscursoDTO> fetchDiscursos(String deputadoId) {
        List<CamaraDeputadoDiscursoDTO> result = new ArrayList<>();
        int pagina = 1;
        while (true) {
            final int p = pagina;
            try {
                CamaraDeputadoDiscursoDTO.ListResponse resp = camaraWebClient.get()
                        .uri(b -> b.path("/api/v2/deputados/{id}/discursos")
                                .queryParam("itens", PAGE_SIZE).queryParam("pagina", p)
                                .build(deputadoId))
                        .retrieve().bodyToMono(CamaraDeputadoDiscursoDTO.ListResponse.class).block();
                if (resp == null || resp.getDados() == null || resp.getDados().isEmpty())
                    break;
                result.addAll(resp.getDados());
                if (!linkHasNext(resp.getLinks()) || resp.getDados().size() < PAGE_SIZE)
                    break;
                pagina++;
            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("[Discursos] Rate limit: dep={}, pag={}", deputadoId, p);
                throw e;
            } catch (Exception e) {
                log.warn("[Discursos] Falha dep={}, pag={}: {}", deputadoId, p, e.getMessage());
                break;
            }
        }
        return result;
    }

    /**
     * Busca todos os eventos de um deputado com paginação automática.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<CamaraDeputadoEventoDTO> fetchEventos(String deputadoId) {
        List<CamaraDeputadoEventoDTO> result = new ArrayList<>();
        int pagina = 1;
        while (true) {
            final int p = pagina;
            try {
                CamaraDeputadoEventoDTO.ListResponse resp = camaraWebClient.get()
                        .uri(b -> b.path("/api/v2/deputados/{id}/eventos")
                                .queryParam("itens", PAGE_SIZE).queryParam("pagina", p)
                                .build(deputadoId))
                        .retrieve().bodyToMono(CamaraDeputadoEventoDTO.ListResponse.class).block();
                if (resp == null || resp.getDados() == null || resp.getDados().isEmpty())
                    break;
                result.addAll(resp.getDados());
                if (!linkHasNext(resp.getLinks()) || resp.getDados().size() < PAGE_SIZE)
                    break;
                pagina++;
            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("[Eventos] Rate limit: dep={}, pag={}", deputadoId, p);
                throw e;
            } catch (Exception e) {
                log.warn("[Eventos] Falha dep={}, pag={}: {}", deputadoId, p, e.getMessage());
                break;
            }
        }
        return result;
    }

    /**
     * Busca todo o histórico de um deputado.
     * Este endpoint não suporta paginação (parâmetro {@code itens} retorna 400);
     * todos os registros são retornados em uma única chamada.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<CamaraDeputadoHistoricoDTO> fetchHistorico(String deputadoId) {
        try {
            CamaraDeputadoHistoricoDTO.ListResponse resp = camaraWebClient.get()
                    .uri("/api/v2/deputados/{id}/historico", deputadoId)
                    .retrieve().bodyToMono(CamaraDeputadoHistoricoDTO.ListResponse.class).block();
            return (resp != null && resp.getDados() != null) ? resp.getDados() : List.of();
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("[Historico] Rate limit: dep={}", deputadoId);
            throw e;
        } catch (Exception e) {
            log.warn("[Historico] Falha dep={}: {}", deputadoId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca todos os mandatos externos de um deputado.
     * Este endpoint não suporta paginação (parâmetro {@code itens} retorna 400);
     * todos os registros são retornados em uma única chamada.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public List<CamaraDeputadoMandatoExternoDTO> fetchMandatosExternos(String deputadoId) {
        try {
            CamaraDeputadoMandatoExternoDTO.ListResponse resp = camaraWebClient.get()
                    .uri("/api/v2/deputados/{id}/mandatosExternos", deputadoId)
                    .retrieve().bodyToMono(CamaraDeputadoMandatoExternoDTO.ListResponse.class).block();
            return (resp != null && resp.getDados() != null) ? resp.getDados() : List.of();
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("[MandatosExternos] Rate limit: dep={}", deputadoId);
            throw e;
        } catch (Exception e) {
            log.warn("[MandatosExternos] Falha dep={}: {}", deputadoId, e.getMessage());
            return List.of();
        }
    }

    // ── Enriquecimento det_* ──────────────────────────────────────────────────

    /**
     * Enriquece todos os deputados que ainda não possuem {@code det_status_id}
     * (flag de enriquecimento pendente) chamando GET /api/v2/deputados/{id}.
     *
     * @param jobControl job ETL corrente
     * @return total de deputados enriquecidos
     */
    public int enriquecerDetalhesTodos(EtlJobControl jobControl) {
        List<SilverCamaraDeputado> pendentes = deputadoRepository.findByDetStatusIdIsNull();
        log.info("[CamaraDeputadoApiExtractor] Iniciando enriquecimento det_* para {} deputados pendentes",
                pendentes.size());

        int enriquecidos = 0, erros = 0;
        for (SilverCamaraDeputado dep : pendentes) {
            try {
                Optional<CamaraDeputadoDetalheDTO> detalhe = fetchDetalhe(dep.getCamaraId());
                if (detalhe.isPresent()) {
                    aplicarDet(dep, detalhe.get());
                    deputadoLoader.salvarEnriquecimento(dep);
                    enriquecidos++;
                } else {
                    dep.setDetStatusId("N/A");
                    deputadoLoader.salvarEnriquecimento(dep);
                    erros++;
                }
            } catch (Exception e) {
                log.warn("[Detalhe] Falha ao enriquecer dep={}: {}", dep.getCamaraId(), e.getMessage());
                erros++;
            }
        }
        log.info("[CamaraDeputadoApiExtractor] Enriquecimento concluído: {} enriquecidos, {} erros",
                enriquecidos, erros);
        return enriquecidos;
    }

    /**
     * Busca o detalhe de um deputado via GET /api/v2/deputados/{id}.
     */
    @RateLimiter(name = "camaraApi")
    @Retry(name = "camaraApi")
    public Optional<CamaraDeputadoDetalheDTO> fetchDetalhe(String deputadoId) {
        try {
            CamaraDeputadoDetalheDTO dto = camaraWebClient.get()
                    .uri("/api/v2/deputados/{id}", deputadoId)
                    .retrieve()
                    .bodyToMono(CamaraDeputadoDetalheDTO.class)
                    .block();
            return Optional.ofNullable(dto);
        } catch (WebClientResponseException.NotFound e) {
            log.debug("[Detalhe] Deputado não encontrado na API: dep={}", deputadoId);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[Detalhe] Falha ao buscar detalhe dep={}: {}", deputadoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Aplica os campos det_* do DTO de detalhe na entidade SilverCamaraDeputado.
     */
    private void aplicarDet(SilverCamaraDeputado dep, CamaraDeputadoDetalheDTO dto) {
        CamaraDeputadoDetalheDTO.Dados d = dto.getDados();
        if (d == null) {
            dep.setDetStatusId("N/A");
            return;
        }

        if (d.getRedeSocial() != null && !d.getRedeSocial().isEmpty()) {
            dep.setDetRedeSocial(toJsonArray(d.getRedeSocial()));
        }

        CamaraDeputadoDetalheDTO.UltimoStatus s = d.getUltimoStatus();
        if (s == null) {
            dep.setDetStatusId("N/A");
            return;
        }

        dep.setDetStatusId(s.getId() != null ? String.valueOf(s.getId()) : "N/A");
        dep.setDetStatusIdLegislatura(
                s.getIdLegislatura() != null ? String.valueOf(s.getIdLegislatura()) : null);
        dep.setDetStatusNome(s.getNome());
        dep.setDetStatusNomeEleitoral(s.getNomeEleitoral());
        dep.setDetStatusSiglaPartido(s.getSiglaPartido());
        dep.setDetStatusSiglaUf(s.getSiglaUf());
        dep.setDetStatusEmail(s.getEmail());
        dep.setDetStatusSituacao(s.getSituacao());
        dep.setDetStatusCondicaoEleitoral(s.getCondicaoEleitoral());
        dep.setDetStatusDescricao(s.getDescricao());
        dep.setDetStatusData(s.getData());
        dep.setDetStatusUriPartido(s.getUriPartido());
        dep.setDetStatusUrlFoto(s.getUrlFoto());

        CamaraDeputadoDetalheDTO.Gabinete g = s.getGabinete();
        if (g != null) {
            dep.setDetGabineteNome(g.getNome());
            dep.setDetGabinetePredio(g.getPredio());
            dep.setDetGabineteSala(g.getSala());
            dep.setDetGabineteAndar(g.getAndar());
            dep.setDetGabineteTelefone(g.getTelefone());
            dep.setDetGabineteEmail(g.getEmail());
        }
    }

    /** Serializa uma lista de strings para JSON array. */
    private static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(",");
            String val = list.get(i) != null ? list.get(i) : "";
            sb.append("\"").append(val.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    /**
     * Verifica se a lista de links de paginação contém um link "next" válido.
     * Compatível com qualquer DTO que possua campos {@code rel} e {@code href} com
     * getters Lombok.
     */
    private static boolean linkHasNext(List<?> links) {
        if (links == null || links.isEmpty())
            return false;
        for (Object l : links) {
            try {
                String rel = (String) l.getClass().getMethod("getRel").invoke(l);
                String href = (String) l.getClass().getMethod("getHref").invoke(l);
                if ("next".equals(rel) && href != null && !href.isBlank())
                    return true;
            } catch (Exception ignored) {
                /* estrutura inesperada — ignora */ }
        }
        return false;
    }
}
