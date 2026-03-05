package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraTramitacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraTramitacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carrega tramitações da Câmara na camada Silver (silver.camara_tramitacao).
 *
 * Princípio Silver: passthrough fiel ao payload de
 * GET /api/v2/proposicoes/{id}/tramitacoes — sem normalização.
 *
 * Deduplicação: chave composta (camara_proposicao_id, sequencia).
 * Tramitações já existentes são ignoradas (imutáveis após registro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraTramitacaoLoader {

    private final SilverCamaraTramitacaoRepository repository;

    /**
     * Persiste as tramitações de uma proposição Silver, ignorando as já existentes.
     *
     * @param proposicao  entidade Silver da proposição
     * @param tramitacoes lista de DTOs retornados pela API da Câmara
     * @return número de tramitações efetivamente inseridas
     */
    @Transactional
    public int carregar(SilverCamaraProposicao proposicao, List<CamaraTramitacaoDTO> tramitacoes) {
        if (proposicao == null || tramitacoes == null || tramitacoes.isEmpty()) {
            return 0;
        }

        int inseridas = 0;
        for (CamaraTramitacaoDTO dto : tramitacoes) {
            if (dto.getSequencia() == null) continue;

            boolean jaExiste = repository.existsByCamaraProposicaoIdAndSequencia(
                proposicao.getId(), dto.getSequencia()
            );

            if (!jaExiste) {
                SilverCamaraTramitacao entity = dtoToEntity(proposicao, dto);
                repository.save(entity);
                inseridas++;
            }
        }

        if (inseridas > 0) {
            log.debug("[Silver] Câmara tramitações: {} novas para proposicaoId={}",
                inseridas, proposicao.getId());
        }

        return inseridas;
    }

    /**
     * Converte um CamaraTramitacaoDTO em entidade Silver.
     * Passthrough puro: nenhum campo é calculado ou normalizado aqui.
     */
    private SilverCamaraTramitacao dtoToEntity(SilverCamaraProposicao proposicao,
                                                CamaraTramitacaoDTO dto) {
        Integer codSituacaoInt = null;
        if (dto.getCodSituacao() != null && !dto.getCodSituacao().isBlank()) {
            try {
                codSituacaoInt = Integer.parseInt(dto.getCodSituacao().trim());
            } catch (NumberFormatException e) {
                log.debug("codSituacao não numérico: '{}'", dto.getCodSituacao());
            }
        }

        return SilverCamaraTramitacao.builder()
            .camaraProposicao(proposicao)
            .sequencia(dto.getSequencia())
            .dataHora(dto.getDataHora())
            .siglaOrgao(dto.getSiglaOrgao())
            .uriOrgao(dto.getUriUltimoOrgao())
            .codTipoTramitacao(dto.getCodTipoTramitacao())
            .descricaoTramitacao(dto.getDescricaoTramitacao())
            .codSituacao(codSituacaoInt)
            .descricaoSituacao(dto.getDescricaoSituacao())
            .despacho(dto.getDespacho())
            .url(dto.getUrl())
            .ambito(dto.getAmbito())
            .apreciacao(dto.getApreciacao())
            .build();
    }
}
