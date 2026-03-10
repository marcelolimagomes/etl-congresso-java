package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorHistoricoAcademico;

/**
 * Repositório Silver — histórico acadêmico por senador.
 * Tabela: silver.senado_senador_historico_academico
 */
@Repository
public interface SilverSenadoSenadorHistoricoAcademicoRepository
        extends JpaRepository<SilverSenadoSenadorHistoricoAcademico, UUID> {

    boolean existsByCodigoSenadorAndCodigoCurso(String codigoSenador, String codigoCurso);
}
