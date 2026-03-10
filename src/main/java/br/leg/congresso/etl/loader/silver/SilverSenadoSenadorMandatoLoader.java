package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorMandato;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorMandatoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorMandatoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega mandatos de senadores na camada Silver
 * (silver.senado_senador_mandato).
 * Deduplicação por (codigo_senador, codigo_mandato).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorMandatoLoader {

    private final SilverSenadoSenadorMandatoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorMandatoDTO.Mandato> mandatos, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || mandatos == null || mandatos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorMandatoDTO.Mandato mandato : mandatos) {
            if (mandato.getCodigoMandato() == null || mandato.getCodigoMandato().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoMandato(codigoSenador, mandato.getCodigoMandato())) {
                repository.save(toEntity(codigoSenador, mandato, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado mandatos senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorMandato toEntity(String codigoSenador,
            SenadoSenadorMandatoDTO.Mandato m, UUID jobId) {
        return SilverSenadoSenadorMandato.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoMandato(m.getCodigoMandato())
                .descricao(m.getDescricaoMandato())
                .ufMandato(m.getUfParlamentar())
                .participacao(m.getDescricaoParticipacao())
                .dataInicio(m.getDataInicio())
                .dataFim(m.getDataFim())
                .dataDesignacao(m.getDataDesignacao())
                .dataTermino(m.getDataTermino())
                .entrouExercicio(m.getEntrouEmExercicio())
                .dataExercicio(m.getDataExercicio())
                .build();
    }
}
