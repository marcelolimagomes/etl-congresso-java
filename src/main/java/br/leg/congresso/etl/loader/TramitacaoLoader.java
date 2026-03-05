package br.leg.congresso.etl.loader;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.repository.TramitacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de carga de tramitações.
 * Substitui as tramitações existentes de uma proposição por uma nova lista.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TramitacaoLoader {

    private final TramitacaoRepository tramitacaoRepository;

    /**
     * Substitui todas as tramitações da proposição informada pela nova lista.
     * A proposição deve já estar persistida (ter ID).
     *
     * @param proposicao  proposição pai (já salva)
     * @param tramitacoes lista de novas tramitações
     */
    @Transactional
    public void carregar(Proposicao proposicao, List<Tramitacao> tramitacoes) {
        if (proposicao.getId() == null) {
            log.warn("Tentativa de carregar tramitações para proposição não persistida: {} {}/{}",
                proposicao.getCasa(), proposicao.getNumero(), proposicao.getAno());
            return;
        }

        if (tramitacoes == null || tramitacoes.isEmpty()) {
            log.debug("Nenhuma tramitação para carregar: proposição={}", proposicao.getId());
            return;
        }

        // Remove tramitações existentes
        tramitacaoRepository.deleteByProposicaoId(proposicao.getId());

        // Associa e persiste as novas
        tramitacoes.forEach(t -> t.setProposicao(proposicao));
        tramitacaoRepository.saveAll(tramitacoes);

        log.debug("Tramitações carregadas: {} registros para proposição {}", tramitacoes.size(), proposicao.getId());
    }
}
