package br.leg.congresso.etl.extractor.senado;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorAparteDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorCargoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorComissaoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDiscursoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorFiliacaoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorHistoricoAcademicoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorLicencaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorMandatoDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorProfissaoDTO;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorAparteLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorCargoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorComissaoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorDiscursoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorFiliacaoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorHistoricoAcademicoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorLicencaLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorMandatoLoader;
import br.leg.congresso.etl.loader.silver.SilverSenadoSenadorProfissaoLoader;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai sub-recursos por senador do Senado Federal e persiste na camada
 * Silver.
 *
 * <p>
 * Para cada senador já presente em {@code silver.senado_senador}, são
 * consultados:
 * <ul>
 * <li>GET /dadosabertos/senador/{codigo}/profissao.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/mandatos.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/licencas.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/historicoAcademico.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/filiacoes.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/discursos.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/comissoes.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/cargos.json</li>
 * <li>GET /dadosabertos/senador/{codigo}/apartes.json</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SenadoSenadorSubrecursosApiExtractor {

    private final @Qualifier("senadoWebClient") WebClient senadoWebClient;

    private final SilverSenadoSenadorRepository senadorRepository;

    private final SilverSenadoSenadorProfissaoLoader profissaoLoader;
    private final SilverSenadoSenadorMandatoLoader mandatoLoader;
    private final SilverSenadoSenadorLicencaLoader licencaLoader;
    private final SilverSenadoSenadorHistoricoAcademicoLoader historicoAcademicoLoader;
    private final SilverSenadoSenadorFiliacaoLoader filiacaoLoader;
    private final SilverSenadoSenadorDiscursoLoader discursoLoader;
    private final SilverSenadoSenadorComissaoLoader comissaoLoader;
    private final SilverSenadoSenadorCargoLoader cargoLoader;
    private final SilverSenadoSenadorAparteLoader aparteLoader;

    private static final String PROFISSAO_PATH = "/dadosabertos/senador/{codigo}/profissao.json";
    private static final String MANDATOS_PATH = "/dadosabertos/senador/{codigo}/mandatos.json";
    private static final String LICENCAS_PATH = "/dadosabertos/senador/{codigo}/licencas.json";
    private static final String HIST_ACAD_PATH = "/dadosabertos/senador/{codigo}/historicoAcademico.json";
    private static final String FILIACOES_PATH = "/dadosabertos/senador/{codigo}/filiacoes.json";
    private static final String DISCURSOS_PATH = "/dadosabertos/senador/{codigo}/discursos.json";
    private static final String COMISSOES_PATH = "/dadosabertos/senador/{codigo}/comissoes.json";
    private static final String CARGOS_PATH = "/dadosabertos/senador/{codigo}/cargos.json";
    private static final String APARTES_PATH = "/dadosabertos/senador/{codigo}/apartes.json";

    // ── Orquestração ─────────────────────────────────────────────────────────

    /**
     * Itera por todos os senadores da Silver e extrai os 9 sub-recursos de cada um.
     *
     * @param jobControl job ETL corrente
     * @return total de registros inseridos
     */
    public int extrairEPersistirTodos(EtlJobControl jobControl) {
        List<SilverSenadoSenador> senadores = senadorRepository.findAll();
        log.info("[SenadoSenadorSubrecursosApiExtractor] Iniciando extração para {} senadores", senadores.size());

        int total = 0;
        for (SilverSenadoSenador senador : senadores) {
            String codigo = senador.getCodigoSenador();
            if (codigo == null || codigo.isBlank()) {
                continue;
            }
            total += extrairSubrecursosPorSenador(codigo, jobControl);
        }

        log.info("[SenadoSenadorSubrecursosApiExtractor] Extração concluída: {} registros inseridos", total);
        return total;
    }

    private int extrairSubrecursosPorSenador(String codigo, EtlJobControl jobControl) {
        int total = 0;
        try {
            total += profissaoLoader.carregar(codigo, fetchProfissoes(codigo), jobControl.getId());
            total += mandatoLoader.carregar(codigo, fetchMandatos(codigo), jobControl.getId());
            total += licencaLoader.carregar(codigo, fetchLicencas(codigo), jobControl.getId());
            total += historicoAcademicoLoader.carregar(codigo, fetchHistoricoAcademico(codigo), jobControl.getId());
            total += filiacaoLoader.carregar(codigo, fetchFiliacoes(codigo), jobControl.getId());
            total += discursoLoader.carregar(codigo, fetchDiscursos(codigo), jobControl.getId());
            total += comissaoLoader.carregar(codigo, fetchComissoes(codigo), jobControl.getId());
            total += cargoLoader.carregar(codigo, fetchCargos(codigo), jobControl.getId());
            total += aparteLoader.carregar(codigo, fetchApartes(codigo), jobControl.getId());
        } catch (Exception e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] Erro ao extrair sub-recursos senador={}: {}",
                    codigo, e.getMessage());
        }
        return total;
    }

    // ── Métodos de fetch individuais ─────────────────────────────────────────

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorProfissaoDTO.Profissao> fetchProfissoes(String codigo) {
        try {
            SenadoSenadorProfissaoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(PROFISSAO_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorProfissaoDTO.class)
                    .block();
            if (response == null || response.getProfissaosParlamentar() == null
                    || response.getProfissaosParlamentar().getParlamentar() == null
                    || response.getProfissaosParlamentar().getParlamentar().getProfissoes() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorProfissaoDTO.Profissao> lista = response.getProfissaosParlamentar().getParlamentar()
                    .getProfissoes().getProfissao();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] profissao senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorMandatoDTO.Mandato> fetchMandatos(String codigo) {
        try {
            SenadoSenadorMandatoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(MANDATOS_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorMandatoDTO.class)
                    .block();
            if (response == null || response.getMandatoParlamentar() == null
                    || response.getMandatoParlamentar().getParlamentar() == null
                    || response.getMandatoParlamentar().getParlamentar().getMandatos() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorMandatoDTO.Mandato> lista = response.getMandatoParlamentar().getParlamentar()
                    .getMandatos().getMandato();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] mandatos senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorLicencaDTO.Licenca> fetchLicencas(String codigo) {
        try {
            SenadoSenadorLicencaDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(LICENCAS_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorLicencaDTO.class)
                    .block();
            if (response == null || response.getLicencaParlamentar() == null
                    || response.getLicencaParlamentar().getParlamentar() == null
                    || response.getLicencaParlamentar().getParlamentar().getLicencas() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorLicencaDTO.Licenca> lista = response.getLicencaParlamentar().getParlamentar()
                    .getLicencas().getLicenca();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] licencas senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorHistoricoAcademicoDTO.Curso> fetchHistoricoAcademico(String codigo) {
        try {
            SenadoSenadorHistoricoAcademicoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(HIST_ACAD_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorHistoricoAcademicoDTO.class)
                    .block();
            if (response == null || response.getHistoricoAcademicoParlamentar() == null
                    || response.getHistoricoAcademicoParlamentar().getParlamentar() == null
                    || response.getHistoricoAcademicoParlamentar().getParlamentar().getHistoricoAcademico() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorHistoricoAcademicoDTO.Curso> lista = response.getHistoricoAcademicoParlamentar()
                    .getParlamentar().getHistoricoAcademico().getCurso();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] historicoAcademico senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorFiliacaoDTO.Filiacao> fetchFiliacoes(String codigo) {
        try {
            SenadoSenadorFiliacaoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(FILIACOES_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorFiliacaoDTO.class)
                    .block();
            if (response == null || response.getFiliacaoParlamentar() == null
                    || response.getFiliacaoParlamentar().getParlamentar() == null
                    || response.getFiliacaoParlamentar().getParlamentar().getFiliacoes() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorFiliacaoDTO.Filiacao> lista = response.getFiliacaoParlamentar().getParlamentar()
                    .getFiliacoes().getFiliacao();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] filiacoes senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorDiscursoDTO.Pronunciamento> fetchDiscursos(String codigo) {
        try {
            SenadoSenadorDiscursoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(DISCURSOS_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorDiscursoDTO.class)
                    .block();
            if (response == null || response.getDiscursosParlamentar() == null
                    || response.getDiscursosParlamentar().getParlamentar() == null
                    || response.getDiscursosParlamentar().getParlamentar().getPronunciamentos() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorDiscursoDTO.Pronunciamento> lista = response.getDiscursosParlamentar().getParlamentar()
                    .getPronunciamentos().getPronunciamento();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] discursos senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorComissaoDTO.Comissao> fetchComissoes(String codigo) {
        try {
            SenadoSenadorComissaoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(COMISSOES_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorComissaoDTO.class)
                    .block();
            if (response == null || response.getMembroComissaoParlamentar() == null
                    || response.getMembroComissaoParlamentar().getParlamentar() == null
                    || response.getMembroComissaoParlamentar().getParlamentar().getMembroComissoes() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorComissaoDTO.Comissao> lista = response.getMembroComissaoParlamentar().getParlamentar()
                    .getMembroComissoes().getComissao();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] comissoes senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorCargoDTO.Cargo> fetchCargos(String codigo) {
        try {
            SenadoSenadorCargoDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(CARGOS_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorCargoDTO.class)
                    .block();
            if (response == null || response.getCargoParlamentar() == null
                    || response.getCargoParlamentar().getParlamentar() == null
                    || response.getCargoParlamentar().getParlamentar().getCargos() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorCargoDTO.Cargo> lista = response.getCargoParlamentar().getParlamentar().getCargos()
                    .getCargo();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] cargos senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @RateLimiter(name = "senadoApi")
    @Retry(name = "senadoApi")
    public List<SenadoSenadorAparteDTO.Aparte> fetchApartes(String codigo) {
        try {
            SenadoSenadorAparteDTO response = senadoWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(APARTES_PATH).build(codigo))
                    .retrieve()
                    .bodyToMono(SenadoSenadorAparteDTO.class)
                    .block();
            if (response == null || response.getApartesParlamentar() == null
                    || response.getApartesParlamentar().getParlamentar() == null
                    || response.getApartesParlamentar().getParlamentar().getApartes() == null) {
                return Collections.emptyList();
            }
            List<SenadoSenadorAparteDTO.Aparte> lista = response.getApartesParlamentar().getParlamentar().getApartes()
                    .getAparte();
            return lista != null ? lista : Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.warn("[SenadoSenadorSubrecursosApiExtractor] apartes senador={}: {} {}",
                    codigo, e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
