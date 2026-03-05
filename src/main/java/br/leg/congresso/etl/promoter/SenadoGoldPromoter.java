package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.loader.ProposicaoLoader;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.transformer.ProposicaoTransformer;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Promove registros Silver do Senado para a camada Gold (tabela proposicao).
 *
 * Responsabilidades:
 * - Transformar SilverSenadoMateria → Proposicao aplicando regras de negócio Gold
 * - Invocar ProposicaoLoader para persistência deduplicada
 * - Marcar goldSincronizado=true nos registros Silver promovidos
 * - Manter rastreabilidade bidirecional via silverSenadoId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SenadoGoldPromoter {

    private final ProposicaoLoader proposicaoLoader;
    private final ProposicaoTransformer transformer;
    private final ProposicaoRepository proposicaoRepository;
    private final SilverSenadoMateriaRepository silverRepository;

    private static final DateTimeFormatter SENADO_DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Promove um lote de registros Silver para Gold e marca cada um como sincronizado.
     *
     * @param registros  lista de SilverSenadoMateria pendentes de promoção
     * @param jobControl controle do job para contadores
     */
    @Transactional
    public void promover(List<SilverSenadoMateria> registros, EtlJobControl jobControl) {
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
            SilverSenadoMateria silver = registros.get(i);
            try {
                // Atualiza FK no Gold (silverSenadoId) — necessário porque o upsert nativo
                // do ProposicaoLoader não inclui a coluna silver_senado_id
                Proposicao gold = proposicoes.get(i);
                if (gold.getIdOrigem() != null) {
                    proposicaoRepository.findByCasaAndIdOrigem(CasaLegislativa.SENADO, gold.getIdOrigem())
                        .ifPresent(p -> {
                            p.setSilverSenadoId(silver.getId());
                            proposicaoRepository.save(p);
                        });
                }
                silverRepository.marcarGoldSincronizado(silver.getId());
            } catch (Exception e) {
                log.warn("Falha ao marcar silver Senado sincronizado (id={}): {}", silver.getId(), e.getMessage());
            }
        }

        log.debug("[Gold Senado] {} registros Silver promovidos", registros.size());
    }

    /**
     * Transforma um registro Silver Senado em entidade Gold.
     */
    private Proposicao silverToGold(SilverSenadoMateria silver) {
        TipoProposicao tipo = TipoProposicaoNormalizer.normalizar(silver.getSigla());

        return Proposicao.builder()
            .casa(CasaLegislativa.SENADO)
            .sigla(silver.getSigla() != null ? silver.getSigla().toUpperCase() : null)
            .numero(parseIntSafe(silver.getNumero()))
            .ano(silver.getAno())
            .tipo(tipo)
            .ementa(silver.getEmenta())
            .dataApresentacao(parseDate(silver.getData()))
            .situacao(silver.getDetIndicadorTramitando())
            .keywords(silver.getDetIndexacao())
            .idOrigem(silver.getCodigo())
            .uriOrigem(silver.getUrlDetalheMateria())
            .silverSenadoId(silver.getId())
            .build();
    }

    private Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String normalized = value.trim();
            if (normalized.contains("/")) {
                return LocalDate.parse(normalized, SENADO_DATE_SLASH);
            }
            return LocalDate.parse(normalized.substring(0, Math.min(10, normalized.length())));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
