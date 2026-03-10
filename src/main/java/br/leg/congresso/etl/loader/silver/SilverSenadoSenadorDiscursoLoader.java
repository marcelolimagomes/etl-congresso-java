package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorDiscurso;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDiscursoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorDiscursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega discursos de senadores na camada Silver
 * (silver.senado_senador_discurso).
 * Deduplicação por (codigo_senador, codigo_discurso).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorDiscursoLoader {

    private final SilverSenadoSenadorDiscursoRepository repository;

    @Transactional
    public int carregar(String codigoSenador,
            List<SenadoSenadorDiscursoDTO.Pronunciamento> pronunciamentos, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank()
                || pronunciamentos == null || pronunciamentos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorDiscursoDTO.Pronunciamento p : pronunciamentos) {
            if (p.getCodigoPronunciamento() == null || p.getCodigoPronunciamento().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoDiscurso(codigoSenador, p.getCodigoPronunciamento())) {
                repository.save(toEntity(codigoSenador, p, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado discursos senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorDiscurso toEntity(String codigoSenador,
            SenadoSenadorDiscursoDTO.Pronunciamento p, UUID jobId) {
        return SilverSenadoSenadorDiscurso.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoDiscurso(p.getCodigoPronunciamento())
                .codigoSessao(p.getCodigoSessao())
                .dataPronunciamento(p.getDataPronunciamento())
                .casa(p.getCasa())
                .tipoSessao(p.getTipoSessao())
                .numeroSessao(p.getNumeroSessao())
                .tipoPronunciamento(p.getTipoPronunciamento())
                .textoDiscurso(p.getTextoPronunciamento())
                .duracaoAparte(p.getDuracaoAparte())
                .urlVideo(p.getUrlVideo())
                .urlAudio(p.getUrlAudio())
                .build();
    }
}
