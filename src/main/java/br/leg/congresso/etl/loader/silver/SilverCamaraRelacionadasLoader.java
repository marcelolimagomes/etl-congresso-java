package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoRelacionada;
import br.leg.congresso.etl.extractor.camara.dto.CamaraRelacionadaDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRelacionadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega proposições relacionadas da Câmara na camada Silver
 * (silver.camara_proposicao_relacionada).
 *
 * Fonte: API GET /api/v2/proposicoes/{id}/relacionadas.
 * Princípio Silver: passthrough fiel à resposta da API — sem normalização.
 * Deduplicação: (proposicao_id, relacionada_id).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraRelacionadasLoader {

    private final SilverCamaraProposicaoRelacionadaRepository repository;

    /**
     * Persiste as proposições relacionadas, ignorando as já existentes.
     *
     * @param proposicao   proposição Silver de origem (pai)
     * @param relacionadas lista de DTOs retornados pela API
     * @param jobId        UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(SilverCamaraProposicao proposicao,
            List<CamaraRelacionadaDTO> relacionadas,
            UUID jobId) {
        if (proposicao == null || relacionadas == null || relacionadas.isEmpty()) {
            return 0;
        }

        String proposicaoId = proposicao.getCamaraId();
        if (proposicaoId == null || proposicaoId.isBlank()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraRelacionadaDTO dto : relacionadas) {
            if (dto.getId() == null) {
                continue;
            }

            Integer relacionadaId = dto.getId().intValue();

            if (!repository.existsByProposicaoIdAndRelacionadaId(proposicaoId, relacionadaId)) {
                repository.save(dtoToEntity(proposicao, dto, proposicaoId, relacionadaId, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara relacionadas: {} novos registros inseridos para proposicaoId={}",
                    inseridos, proposicaoId);
        }

        return inseridos;
    }

    private SilverCamaraProposicaoRelacionada dtoToEntity(SilverCamaraProposicao proposicao,
            CamaraRelacionadaDTO dto,
            String proposicaoId,
            Integer relacionadaId,
            UUID jobId) {
        return SilverCamaraProposicaoRelacionada.builder()
                .etlJobId(jobId)
                .proposicaoId(proposicaoId)
                .camaraProposicao(proposicao)
                .relacionadaId(relacionadaId)
                .relacionadaUri(dto.getUri())
                .relacionadaSiglaTipo(dto.getSiglaTipo())
                .relacionadaNumero(dto.getNumero() != null ? dto.getNumero().toString() : null)
                .relacionadaAno(dto.getAno() != null ? dto.getAno().toString() : null)
                .relacionadaEmenta(dto.getEmenta())
                .relacionadaCodTipo(dto.getCodTipo() != null ? dto.getCodTipo().toString() : null)
                .build();
    }
}
