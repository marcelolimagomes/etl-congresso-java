package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorMandato;

/**
 * Repositório Silver — mandatos por senador.
 * Tabela: silver.senado_senador_mandato
 */
@Repository
public interface SilverSenadoSenadorMandatoRepository
        extends JpaRepository<SilverSenadoSenadorMandato, UUID> {

    boolean existsByCodigoSenadorAndCodigoMandato(String codigoSenador, String codigoMandato);
}
