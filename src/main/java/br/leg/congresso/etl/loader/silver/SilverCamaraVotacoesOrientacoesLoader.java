package br.leg.congresso.etl.loader.silver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoOrientacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoOrientacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoOrientacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega orientações de bancada para votações da Câmara na camada Silver
 * (silver.camara_votacao_orientacao).
 *
 * Princípio Silver: passthrough fiel ao CSV votacoesOrientacoes-{ano}.csv.
 * Deduplicação: (id_votacao, sigla_bancada).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraVotacoesOrientacoesLoader {

    private final SilverCamaraVotacaoOrientacaoRepository repository;

    @Transactional
    public int carregar(List<CamaraVotacaoOrientacaoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        Set<String> idsVotacao = rows.stream()
                .map(CamaraVotacaoOrientacaoCSVRow::getIdVotacao)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        Set<String> chavesExistentes = repository.findAllByIdVotacaoIn(idsVotacao).stream()
                .map(e -> buildKey(e.getIdVotacao(), e.getSiglaBancada()))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        int inseridos = 0;
        for (CamaraVotacaoOrientacaoCSVRow row : rows) {
            if (row.getIdVotacao() == null || row.getIdVotacao().isBlank()
                    || row.getSiglaBancada() == null || row.getSiglaBancada().isBlank()) {
                continue;
            }

            String chave = buildKey(row.getIdVotacao(), row.getSiglaBancada());
            if (!chavesExistentes.contains(chave)) {
                SilverCamaraVotacaoOrientacao entity = rowToEntity(row, jobId);
                if (entity == null) {
                    continue;
                }
                repository.save(entity);
                chavesExistentes.add(chave);
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara votações orientações: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraVotacaoOrientacao rowToEntity(CamaraVotacaoOrientacaoCSVRow row, UUID jobId) {
        return SilverCamaraVotacaoOrientacao.builder()
                .etlJobId(jobId)
                .idVotacao(row.getIdVotacao())
                .uriVotacao(row.getUriVotacao())
                .siglaOrgao(row.getSiglaOrgao())
                .descricao(row.getDescricao())
                .siglaBancada(row.getSiglaBancada())
                .uriBancada(row.getUriBancada())
                .orientacao(row.getOrientacao())
                .build();
    }

    private String buildKey(String idVotacao, String siglaBancada) {
        return idVotacao + "|" + siglaBancada;
    }
}
