package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorLicenca;

/**
 * Repositório Silver — licenças por senador.
 * Tabela: silver.senado_senador_licenca
 */
@Repository
public interface SilverSenadoSenadorLicencaRepository
        extends JpaRepository<SilverSenadoSenadorLicenca, UUID> {

    boolean existsByCodigoSenadorAndCodigoLicenca(String codigoSenador, String codigoLicenca);
}
