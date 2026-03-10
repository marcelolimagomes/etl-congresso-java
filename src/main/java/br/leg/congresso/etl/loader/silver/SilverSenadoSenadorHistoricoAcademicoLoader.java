package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorHistoricoAcademico;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorHistoricoAcademicoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorHistoricoAcademicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega histórico acadêmico de senadores na camada Silver
 * (silver.senado_senador_historico_academico).
 * Deduplicação por (codigo_senador, codigo_curso).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorHistoricoAcademicoLoader {

    private final SilverSenadoSenadorHistoricoAcademicoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorHistoricoAcademicoDTO.Curso> cursos, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || cursos == null || cursos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorHistoricoAcademicoDTO.Curso curso : cursos) {
            if (curso.getCodigoCurso() == null || curso.getCodigoCurso().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoCurso(codigoSenador, curso.getCodigoCurso())) {
                repository.save(toEntity(codigoSenador, curso, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado histórico acadêmico senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorHistoricoAcademico toEntity(String codigoSenador,
            SenadoSenadorHistoricoAcademicoDTO.Curso c, UUID jobId) {
        return SilverSenadoSenadorHistoricoAcademico.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoCurso(c.getCodigoCurso())
                .nomeCurso(c.getNomeCurso())
                .instituicao(c.getInstituicao())
                .descricaoInstituicao(c.getDescricaoInstituicao())
                .nivelFormacao(c.getGrauInstrucao())
                .dataInicioFormacao(c.getDataInicioCurso())
                .dataTerminoFormacao(c.getDataTerminoCurso())
                .concluido(c.getConcluido())
                .build();
    }
}
