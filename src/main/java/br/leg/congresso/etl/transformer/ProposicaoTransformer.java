package br.leg.congresso.etl.transformer;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de transformação que enriquece proposições com hash de conteúdo.
 * Atua como ponte entre a camada de extração e a camada de persistência.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposicaoTransformer {

    private final ContentHashGenerator hashGenerator;
    private final DeduplicationService deduplicationService;

    /**
     * Enriquece uma proposicão com hash de conteúdo e campos de controle.
     *
     * @param proposicao a proposição a ser enriquecida
     * @return proposição com contentHash preenchido
     */
    public Proposicao enriquecer(Proposicao proposicao) {
        // 1. Normaliza sigla (garante maiúsculas)
        if (proposicao.getSigla() != null) {
            proposicao.setSigla(proposicao.getSigla().toUpperCase().trim());
        }

        // 2. Gera hash de conteúdo para deduplicação
        deduplicationService.enriquecerComHash(proposicao);

        return proposicao;
    }

    /**
     * Enriquece um lote de proposições.
     *
     * @param proposicoes lista de proposições a enriquecer
     * @return a mesma lista, com contentHash preenchido em cada item
     */
    public List<Proposicao> enriquecerLote(List<Proposicao> proposicoes) {
        proposicoes.forEach(this::enriquecer);
        return proposicoes;
    }

    /**
     * Associa tramitações à proposição.
     *
     * @param proposicao  a proposição pai
     * @param tramitacoes lista de tramitações a associar
     */
    public void associarTramitacoes(Proposicao proposicao, List<Tramitacao> tramitacoes) {
        tramitacoes.forEach(t -> t.setProposicao(proposicao));
        proposicao.setTramitacoes(tramitacoes);
    }
}
