package br.leg.congresso.etl.loader;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Helper para upsert em lote de proposições.
 * Utiliza a função nativa de upsert com ON CONFLICT DO UPDATE para garantir idempotência.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchUpsertHelper {

    private final ProposicaoRepository proposicaoRepository;

    /**
     * Executa upsert de uma lista de proposições em uma transação separada.
     * Cada proposição é inserida ou atualizada conforme o conteúdo via content_hash.
     *
     * @param proposicoes lista de proposições a persistir
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(List<Proposicao> proposicoes) {
        if (proposicoes == null || proposicoes.isEmpty()) return;

        try {
            // Usa o método nativo de upsert do repositório (uma proposição por vez para ON CONFLICT)
            for (Proposicao p : proposicoes) {
                proposicaoRepository.upsert(
                    p.getCasa()  != null ? p.getCasa().name()  : null,
                    p.getTipo()  != null ? p.getTipo().name()  : null,
                    p.getSigla(),
                    p.getNumero(),
                    p.getAno(),
                    p.getEmenta(),
                    p.getSituacao(),
                    p.getDespachoAtual(),
                    p.getDataApresentacao(),
                    p.getDataAtualizacao(),
                    p.getStatusFinal(),
                    p.getIdOrigem(),
                    p.getUriOrigem(),
                    p.getUrlInteiroTeor(),
                    p.getKeywords(),
                    p.getContentHash()
                );
            }
            log.debug("Upsert concluído para {} proposições", proposicoes.size());
        } catch (Exception e) {
            log.error("Falha no upsert de {} proposições: {}", proposicoes.size(), e.getMessage(), e);
            throw e;
        }
    }
}
