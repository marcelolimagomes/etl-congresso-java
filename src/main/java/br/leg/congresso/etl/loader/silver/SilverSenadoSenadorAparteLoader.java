package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAparte;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorAparteDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorAparteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega apartes de senadores na camada Silver
 * (silver.senado_senador_aparte).
 * Deduplicação por (codigo_senador, codigo_aparte).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorAparteLoader {

    private final SilverSenadoSenadorAparteRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorAparteDTO.Aparte> apartes, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || apartes == null || apartes.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorAparteDTO.Aparte aparte : apartes) {
            if (aparte.getCodigoAparte() == null || aparte.getCodigoAparte().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoAparte(codigoSenador, aparte.getCodigoAparte())) {
                repository.save(toEntity(codigoSenador, aparte, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado apartes senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorAparte toEntity(String codigoSenador,
            SenadoSenadorAparteDTO.Aparte a, UUID jobId) {
        return SilverSenadoSenadorAparte.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoAparte(a.getCodigoAparte())
                .codigoDiscursoPrincipal(a.getCodigoPronunciamentoPrincipal())
                .codigoSessao(a.getCodigoSessao())
                .dataPronunciamento(a.getDataPronunciamento())
                .casa(a.getCasa())
                .textoAparte(a.getTextoAparte())
                .urlVideo(a.getUrlVideo())
                .build();
    }
}
