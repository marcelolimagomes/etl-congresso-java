package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoVotacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoVotacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoVotacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega votações de matérias do Senado na camada Silver
 * (silver.senado_votacao).
 *
 * Princípio Silver: passthrough fiel ao payload de GET
 * /dadosabertos/votacao?codigoMateria={codigo}.
 * Deduplicação: (senado_materia_id, codigo_sessao_votacao, sequencial_sessao).
 * Votos dos parlamentares são armazenados como JSONB (passthrough).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoVotacaoLoader {

    private final SilverSenadoVotacaoRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoVotacaoDTO> votacoes, UUID jobId) {
        if (materia == null || votacoes == null || votacoes.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoVotacaoDTO dto : votacoes) {
            String codigoSessaoVotacao = dto.getCodigoSessaoVotacao();
            if (codigoSessaoVotacao == null || codigoSessaoVotacao.isBlank()) {
                continue;
            }

            String sequencialSessao = dto.getSequencialSessao();

            if (!repository.existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
                    materia.getId(), codigoSessaoVotacao, sequencialSessao)) {
                repository.save(dtoToEntity(materia, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado votações: {} novos registros inseridos para codigo={}", inseridos,
                    materia.getCodigo());
        }

        return inseridos;
    }

    private SilverSenadoVotacao dtoToEntity(SilverSenadoMateria materia, SenadoVotacaoDTO dto, UUID jobId) {
        String votosJson = null;
        if (dto.getVotosParlamentares() != null && !dto.getVotosParlamentares().isEmpty()) {
            try {
                votosJson = objectMapper.writeValueAsString(dto.getVotosParlamentares());
            } catch (JsonProcessingException e) {
                log.warn("[Silver] Falha ao serializar votos parlamentares: {}", e.getMessage());
            }
        }

        return SilverSenadoVotacao.builder()
                .senadoMateria(materia)
                .etlJobId(jobId)
                .codigoMateria(materia.getCodigo())
                .codigoSessao(dto.getCodigoSessao())
                .siglaCasa(dto.getSiglaCasa())
                .codigoSessaoVotacao(dto.getCodigoSessaoVotacao())
                .sequencialSessao(dto.getSequencialSessao())
                .dataSessao(dto.getDataSessao())
                .descricaoVotacao(dto.getDescricaoVotacao())
                .resultado(dto.getResultado())
                .descricaoResultado(dto.getDescricaoResultado())
                .totalVotosSim(dto.getTotalVotosSim())
                .totalVotosNao(dto.getTotalVotosNao())
                .totalVotosAbstencao(dto.getTotalVotosAbstencao())
                .indicadorVotacaoSecreta(dto.getIndicadorVotacaoSecreta())
                .votosParlamentares(votosJson)
                .build();
    }
}
