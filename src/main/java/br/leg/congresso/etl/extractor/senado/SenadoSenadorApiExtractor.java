package br.leg.congresso.etl.extractor.senado;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAfastadoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPartidoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorListaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoTipoUsoPalavraDTO;
import br.leg.congresso.etl.loader.silver.SilverSenadoPartidoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorAfastadoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoTipoUsoPalavraLoader;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai dados de senadores, partidos e tipos de uso de palavra do Senado
 * Federal
 * via API REST e persiste na camada Silver.
 *
 * <p>
 * Endpoints cobertos:
 * <ul>
 * <li>GET /dadosabertos/senador/lista/atual.json → silver.senado_senador</li>
 * <li>GET /dadosabertos/senador/{codigo}.json → silver.senado_senador
 * (det_*)</li>
 * <li>GET /dadosabertos/senador/afastados.json →
 * silver.senado_senador_afastado</li>
 * <li>GET /dadosabertos/senador/partidos.json → silver.senado_partido</li>
 * <li>GET /dadosabertos/senador/lista/tiposUsoPalavra.json →
 * silver.senado_tipo_uso_palavra</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SenadoSenadorApiExtractor {

    private final @Qualifier("senadoWebClient") WebClient senadoWebClient;

    private final SilverSenadoSenadorLoader senadorLoader;
    private final SilverSenadoSenadorAfastadoLoader afastadoLoader;
    private final SilverSenadoPartidoLoader partidoLoader;
    private final SilverSenadoTipoUsoPalavraLoader tipoUsoPalavraLoader;

    @Value("${etl.senado.enrich-senador-detalhe:true}")
    private boolean enrichSenadorDetalhe;

    private static final String LISTA_ATUAL_PATH = "/dadosabertos/senador/lista/atual.json";
    private static final String AFASTADOS_PATH = "/dadosabertos/senador/afastados.json";
    private static final String PARTIDOS_PATH = "/dadosabertos/senador/partidos.json";
    private static final String TIPOS_USO_PALAVRA_PATH = "/dadosabertos/senador/lista/tiposUsoPalavra.json";
    private static final String DETALHE_PATH = "/dadosabertos/senador/{codigo}.json";

    // ── Orquestração ─────────────────────────────────────────────────────────

    /**
     * Extrai e persiste todos os dados de senadores, afastados, partidos e tipos de
     * uso de palavra.
     *
     * @param jobControl job ETL corrente — usado para rastreabilidade
     * @return total de registros inseridos/atualizados
     */
    public int extrairEPersistirTodos(EtlJobControl jobControl) {
        log.info("[SenadoSenadorApiExtractor] Iniciando extração");

        int total = 0;

        // 1. Senadores em exercício
        List<SenadoSenadorListaDTO.Parlamentar> senadores = fetchSenadoresAtuais();
        total += senadorLoader.carregar(senadores, jobControl.getId());
        log.info("[SenadoSenadorApiExtractor] Senadores carregados: {}", senadores.size());

        // 2. Detalhe individual (det_* complement) — opcional via config
        if (enrichSenadorDetalhe) {
            int detAtualizados = 0;
            for (SenadoSenadorListaDTO.Parlamentar p : senadores) {
                if (p.getIdentificacaoParlamentar() == null)
                    continue;
                String codigo = p.getIdentificacaoParlamentar().getCodigoParlamentar();
                if (codigo == null || codigo.isBlank())
                    continue;

                try {
                    SenadoSenadorDetalheDTO detalhe = fetchDetalhe(codigo);
                    if (senadorLoader.carregarDetalhe(codigo, detalhe, jobControl.getId())) {
                        detAtualizados++;
                    }
                } catch (Exception e) {
                    log.warn("[SenadoSenadorApiExtractor] Detalhes indisponíveis para senador={}: {}",
                            codigo, e.getMessage());
                }
            }
            log.info("[SenadoSenadorApiExtractor] Detalhes de senadores atualizados: {}", detAtualizados);
        }

        // 3. Afastados
        List<SenadoAfastadoDTO.Parlamentar> afastados = fetchAfastados();
        total += afastadoLoader.carregar(afastados, jobControl.getId());
        log.info("[SenadoSenadorApiExtractor] Afastados carregados: {}", afastados.size());

        // 4. Partidos
        List<SenadoPartidoDTO.Partido> partidos = fetchPartidos();
        total += partidoLoader.carregar(partidos, jobControl.getId());
        log.info("[SenadoSenadorApiExtractor] Partidos carregados: {}", partidos.size());

        // 5. Tipos de uso de palavra
        List<SenadoTipoUsoPalavraDTO.TipoUsoPalavra> tipos = fetchTiposUsoPalavra();
        total += tipoUsoPalavraLoader.carregar(tipos, jobControl.getId());
        log.info("[SenadoSenadorApiExtractor] Tipos de uso de palavra carregados: {}", tipos.size());

        log.info("[SenadoSenadorApiExtractor] Extração concluída: {} registros inseridos no total", total);
        return total;
    }

    // ── Métodos de fetch individuais ─────────────────────────────────────────

    /**
     * Busca a lista de senadores em exercício.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorListaDTO.Parlamentar> fetchSenadoresAtuais() {
        try {
            SenadoSenadorListaDTO response = senadoWebClient.get()
                    .uri(LISTA_ATUAL_PATH)
                    .retrieve()
                    .bodyToMono(SenadoSenadorListaDTO.class)
                    .block();

            if (response == null
                    || response.getListaParlamentarEmExercicio() == null
                    || response.getListaParlamentarEmExercicio().getParlamentares() == null) {
                return Collections.emptyList();
            }

            List<SenadoSenadorListaDTO.Parlamentar> lista = response.getListaParlamentarEmExercicio().getParlamentares()
                    .getParlamentar();
            return lista != null ? lista : Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("[SenadoSenadorApiExtractor] Erro ao buscar lista/atual: {} {}", e.getStatusCode(),
                    e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca o detalhe de um único senador.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public SenadoSenadorDetalheDTO fetchDetalhe(String codigoSenador) {
        return senadoWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(DETALHE_PATH).build(codigoSenador))
                .retrieve()
                .bodyToMono(SenadoSenadorDetalheDTO.class)
                .block();
    }

    /**
     * Busca a lista de senadores afastados.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoAfastadoDTO.Parlamentar> fetchAfastados() {
        try {
            SenadoAfastadoDTO response = senadoWebClient.get()
                    .uri(AFASTADOS_PATH)
                    .retrieve()
                    .bodyToMono(SenadoAfastadoDTO.class)
                    .block();

            if (response == null
                    || response.getAfastamentoParlamentar() == null
                    || response.getAfastamentoParlamentar().getParlamentares() == null) {
                return Collections.emptyList();
            }

            List<SenadoAfastadoDTO.Parlamentar> lista = response.getAfastamentoParlamentar().getParlamentares()
                    .getParlamentar();
            return lista != null ? lista : Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("[SenadoSenadorApiExtractor] Erro ao buscar afastados: {} {}", e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de partidos.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoPartidoDTO.Partido> fetchPartidos() {
        try {
            SenadoPartidoDTO response = senadoWebClient.get()
                    .uri(PARTIDOS_PATH)
                    .retrieve()
                    .bodyToMono(SenadoPartidoDTO.class)
                    .block();

            if (response == null
                    || response.getPartidos() == null) {
                return Collections.emptyList();
            }

            List<SenadoPartidoDTO.Partido> lista = response.getPartidos().getPartido();
            return lista != null ? lista : Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("[SenadoSenadorApiExtractor] Erro ao buscar partidos: {} {}", e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Busca a lista de tipos de uso de palavra.
     */
    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoTipoUsoPalavraDTO.TipoUsoPalavra> fetchTiposUsoPalavra() {
        try {
            SenadoTipoUsoPalavraDTO response = senadoWebClient.get()
                    .uri(TIPOS_USO_PALAVRA_PATH)
                    .retrieve()
                    .bodyToMono(SenadoTipoUsoPalavraDTO.class)
                    .block();

            if (response == null
                    || response.getTiposUsoPalavra() == null) {
                return Collections.emptyList();
            }

            List<SenadoTipoUsoPalavraDTO.TipoUsoPalavra> lista = response.getTiposUsoPalavra().getTipoUsoPalavra();
            return lista != null ? lista : Collections.emptyList();

        } catch (WebClientResponseException e) {
            log.error("[SenadoSenadorApiExtractor] Erro ao buscar tiposUsoPalavra: {} {}", e.getStatusCode(),
                    e.getMessage());
            return Collections.emptyList();
        }
    }
}
