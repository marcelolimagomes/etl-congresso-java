package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.loader.ProposicaoLoader;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.transformer.ProposicaoTransformer;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Promove registros Silver da Câmara para a camada Gold (tabela proposicao).
 *
 * Responsabilidades:
 * - Transformar SilverCamaraProposicao → Proposicao aplicando regras de negócio Gold
 * - Invocar ProposicaoLoader para persistência deduplicada
 * - Marcar goldSincronizado=true nos registros Silver promovidos
 * - Manter rastreabilidade bidirecional via silverCamaraId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CamaraGoldPromoter {

    private final ProposicaoLoader proposicaoLoader;
    private final ProposicaoTransformer transformer;
    private final ProposicaoRepository proposicaoRepository;
    private final SilverCamaraProposicaoRepository silverRepository;

    /**
     * Promove um lote de registros Silver para Gold e marca cada um como sincronizado.
     *
     * @param registros  lista de SilverCamaraProposicao pendentes de promoção
     * @param jobControl controle do job para contadores
     */
    @Transactional
    public void promover(List<SilverCamaraProposicao> registros, EtlJobControl jobControl) {
        if (registros == null || registros.isEmpty()) return;

        List<Proposicao> proposicoes = registros.stream()
            .map(this::silverToGold)
            .toList();

        // Enriquece com hash de conteúdo
        transformer.enriquecerLote(proposicoes);

        // Persiste no Gold via upsert deduplicado
        proposicaoLoader.carregar(proposicoes, jobControl);

        // Marca Silver como promovido e estabelece rastreabilidade
        for (int i = 0; i < registros.size(); i++) {
            SilverCamaraProposicao silver = registros.get(i);
            try {
                // Atualiza FK no Gold (silverCamaraId) — necessário porque o upsert nativo
                // do ProposicaoLoader não inclui a coluna silver_camara_id
                Proposicao gold = proposicoes.get(i);
                if (gold.getIdOrigem() != null) {
                    proposicaoRepository.findByCasaAndIdOrigem(CasaLegislativa.CAMARA, gold.getIdOrigem())
                        .ifPresent(p -> {
                            p.setSilverCamaraId(silver.getId());
                            proposicaoRepository.save(p);
                        });
                }
                // Marca Silver como sincronizado
                silverRepository.marcarGoldSincronizado(silver.getId());
            } catch (Exception e) {
                log.warn("Falha ao marcar silver sincronizado (id={}): {}", silver.getId(), e.getMessage());
            }
        }

        log.debug("[Gold Câmara] {} registros Silver promovidos", registros.size());
    }

    /**
     * Transforma um registro Silver em entidade Gold.
     * Aplica normalização de sigla, tipo e campos de controle.
     */
    private Proposicao silverToGold(SilverCamaraProposicao silver) {
        TipoProposicao tipo = TipoProposicaoNormalizer.normalizar(silver.getSiglaTipo());

        return Proposicao.builder()
            .casa(CasaLegislativa.CAMARA)
            .sigla(silver.getSiglaTipo() != null ? silver.getSiglaTipo().toUpperCase() : null)
            .numero(silver.getNumero())
            .ano(silver.getAno())
            .tipo(tipo)
            .ementa(resolveEmenta(silver))
            .dataApresentacao(parseDate(silver.getDataApresentacao()))
            .situacao(silver.getUltimoStatusDescricaoSituacao())
            .despachoAtual(silver.getUltimoStatusDespacho())
            .dataAtualizacao(parseDateTime(silver.getUltimoStatusDataHora()))
            .idOrigem(silver.getCamaraId())
            .uriOrigem(silver.getUri())
            .urlInteiroTeor(silver.getUrlInteiroTeor())
            .keywords(silver.getKeywords())
            .silverCamaraId(silver.getId())
            .build();
    }

    private String resolveEmenta(SilverCamaraProposicao silver) {
        if (silver.getEmenta() != null && !silver.getEmenta().isBlank()) {
            return silver.getEmenta();
        }
        return silver.getEmentaDetalhada();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            try {
                LocalDate d = parseDate(value);
                return d != null ? d.atStartOfDay() : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
