package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMovimentacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoMovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carrega movimentações do Senado na camada Silver (silver.senado_movimentacao).
 *
 * Princípio Silver: passthrough fiel ao payload de
 * GET /dadosabertos/materia/{id}/movimentacoes.json — sem normalização.
 *
 * Deduplicação: chave composta (senado_materia_id, sequencia_movimentacao).
 * Movimentações já existentes são ignoradas (imutáveis após registro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoMovimentacaoLoader {

    private final SilverSenadoMovimentacaoRepository repository;

    /**
     * Persiste as movimentações de uma matéria Silver, ignorando as já existentes.
     *
     * @param materia       entidade Silver da matéria
     * @param movimentacoes lista de DTOs retornados pela API do Senado
     * @return número de movimentações efetivamente inseridas
     */
    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoMovimentacaoDTO.Movimentacao> movimentacoes) {
        if (materia == null || movimentacoes == null || movimentacoes.isEmpty()) {
            return 0;
        }

        int inseridas = 0;
        for (SenadoMovimentacaoDTO.Movimentacao dto : movimentacoes) {
            if (dto.getSequenciaMovimentacao() == null) continue;

            boolean jaExiste = repository.existsBySenadoMateriaIdAndSequenciaMovimentacao(
                materia.getId(), dto.getSequenciaMovimentacao()
            );

            if (!jaExiste) {
                SilverSenadoMovimentacao entity = dtoToEntity(materia, dto);
                repository.save(entity);
                inseridas++;
            }
        }

        if (inseridas > 0) {
            log.debug("[Silver] Senado movimentações: {} novas para materiaId={}",
                inseridas, materia.getId());
        }

        return inseridas;
    }

    /**
     * Converte um Movimentacao DTO em entidade Silver.
     * Passthrough puro: nenhum campo é calculado ou normalizado aqui.
     */
    private SilverSenadoMovimentacao dtoToEntity(SilverSenadoMateria materia,
                                                   SenadoMovimentacaoDTO.Movimentacao dto) {
        String siglaLocal = null;
        String nomeLocal = null;
        if (dto.getLocal() != null) {
            siglaLocal = dto.getLocal().getSiglaLocal();
            nomeLocal  = dto.getLocal().getNomeLocal();
        }

        return SilverSenadoMovimentacao.builder()
            .senadoMateria(materia)
            .sequenciaMovimentacao(dto.getSequenciaMovimentacao())
            .dataMovimentacao(dto.getDataMovimentacao())
            .descricaoMovimentacao(dto.getDescricaoMovimentacao())
            .descricaoSituacao(dto.getDescricaoSituacao())
            .despacho(dto.getDespacho())
            .ambito(dto.getAmbito())
            .siglaLocal(siglaLocal)
            .nomeLocal(nomeLocal)
            .build();
    }
}
