package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorFiliacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorFiliacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorFiliacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega filiações partidárias de senadores na camada Silver
 * (silver.senado_senador_filiacao).
 * Deduplicação por (codigo_senador, codigo_filiacao).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorFiliacaoLoader {

    private final SilverSenadoSenadorFiliacaoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorFiliacaoDTO.Filiacao> filiacoes, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || filiacoes == null || filiacoes.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorFiliacaoDTO.Filiacao filiacao : filiacoes) {
            if (filiacao.getCodigoFiliacao() == null || filiacao.getCodigoFiliacao().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoFiliacao(codigoSenador, filiacao.getCodigoFiliacao())) {
                repository.save(toEntity(codigoSenador, filiacao, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado filiações senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorFiliacao toEntity(String codigoSenador,
            SenadoSenadorFiliacaoDTO.Filiacao f, UUID jobId) {
        String codigoPartido = null;
        String siglaPartido = null;
        String nomePartido = null;
        if (f.getPartido() != null) {
            codigoPartido = f.getPartido().getCodigoPartido();
            siglaPartido = f.getPartido().getSiglaPartido();
            nomePartido = f.getPartido().getNomePartido();
        }
        return SilverSenadoSenadorFiliacao.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoFiliacao(f.getCodigoFiliacao())
                .codigoPartido(codigoPartido)
                .siglaPartido(siglaPartido)
                .nomePartido(nomePartido)
                .dataInicioFiliacao(f.getDataFiliacao())
                .dataTerminoFiliacao(f.getDataDesfiliacao())
                .build();
    }
}
