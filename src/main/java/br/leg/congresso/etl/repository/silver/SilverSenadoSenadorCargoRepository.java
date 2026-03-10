package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorCargo;

/**
 * Repositório Silver — cargos por senador.
 * Tabela: silver.senado_senador_cargo
 */
@Repository
public interface SilverSenadoSenadorCargoRepository
        extends JpaRepository<SilverSenadoSenadorCargo, UUID> {

    boolean existsByCodigoSenadorAndCodigoCargoAndDataInicio(
            String codigoSenador, String codigoCargo, String dataInicio);
}
