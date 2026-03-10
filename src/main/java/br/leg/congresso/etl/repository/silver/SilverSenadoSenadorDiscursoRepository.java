package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorDiscurso;

/**
 * Repositório Silver — discursos por senador.
 * Tabela: silver.senado_senador_discurso
 */
@Repository
public interface SilverSenadoSenadorDiscursoRepository
        extends JpaRepository<SilverSenadoSenadorDiscurso, UUID> {

    boolean existsByCodigoSenadorAndCodigoDiscurso(String codigoSenador, String codigoDiscurso);
}
