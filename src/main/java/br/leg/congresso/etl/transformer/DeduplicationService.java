package br.leg.congresso.etl.transformer;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Decide se um registro deve ser INSERTado, UPDATE ou ignorado (SKIP)
 * com base na chave natural e no content_hash.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private final ProposicaoRepository proposicaoRepository;
    private final ContentHashGenerator hashGenerator;

    public enum Acao { INSERT, UPDATE, SKIP }

    /**
     * Avalia o que fazer com a proposição:
     * - Se não existir: INSERT
     * - Se existir com mesmo hash: SKIP
     * - Se existir com hash diferente: UPDATE
     */
    @Transactional(readOnly = true)
    public Acao avaliar(Proposicao candidata) {
        CasaLegislativa casa   = candidata.getCasa();
        String sigla           = candidata.getSigla();
        Integer numero         = candidata.getNumero();
        Integer ano            = candidata.getAno();

        if (sigla == null || numero == null || ano == null) {
            log.warn("[DEDUP] Proposição com chave incompleta ignorada: {} {}/{}", casa, sigla, ano);
            return Acao.SKIP;
        }

        Optional<String> hashExistente = proposicaoRepository
            .findContentHashByChaveNatural(casa, sigla, numero, ano);

        if (hashExistente.isEmpty()) {
            return Acao.INSERT;
        }

        if (hashExistente.get().equals(candidata.getContentHash())) {
            return Acao.SKIP;
        }

        return Acao.UPDATE;
    }

    /**
     * Enriquece a proposição com o hash de conteúdo antes de persistir.
     */
    public void enriquecerComHash(Proposicao proposicao) {
        normalizarIdOrigem(proposicao);

        String hash = hashGenerator.generateForProposicao(
            proposicao.getCasa() != null ? proposicao.getCasa().name() : null,
            proposicao.getSigla(),
            proposicao.getNumero(),
            proposicao.getAno(),
            proposicao.getEmenta(),
            proposicao.getSituacao(),
            proposicao.isVirouLei(),
            proposicao.getIdOrigem(),
            proposicao.getUriOrigem(),
            proposicao.getDespachoAtual(),
            proposicao.getStatusFinal(),
            proposicao.getUrlInteiroTeor(),
            proposicao.getKeywords()
        );
        proposicao.setContentHash(hash);
    }

    private void normalizarIdOrigem(Proposicao proposicao) {
        if (proposicao.getIdOrigem() != null && !proposicao.getIdOrigem().isBlank()) {
            return;
        }

        String uriOrigem = proposicao.getUriOrigem();
        if (uriOrigem == null || uriOrigem.isBlank()) {
            return;
        }

        String trimmed = uriOrigem.trim();
        int idx = trimmed.lastIndexOf('/');
        if (idx >= 0 && idx < trimmed.length() - 1) {
            String candidate = trimmed.substring(idx + 1).trim();
            if (!candidate.isEmpty()) {
                proposicao.setIdOrigem(candidate);
            }
        }
    }
}
