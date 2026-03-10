package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAparte;

/**
 * Repositório Silver — apartes por senador.
 * Tabela: silver.senado_senador_aparte
 */
@Repository
public interface SilverSenadoSenadorAparteRepository
        extends JpaRepository<SilverSenadoSenadorAparte, UUID> {

    boolean existsByCodigoSenadorAndCodigoAparte(String codigoSenador, String codigoAparte);
}
