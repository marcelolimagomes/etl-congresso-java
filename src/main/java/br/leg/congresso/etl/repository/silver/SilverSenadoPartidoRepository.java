package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoPartido;

/**
 * Repositório Silver — partidos do Senado Federal.
 * Tabela: silver.senado_partido
 */
@Repository
public interface SilverSenadoPartidoRepository
        extends JpaRepository<SilverSenadoPartido, UUID> {

    /**
     * Verifica se já existe partido com o código fornecido.
     */
    boolean existsByCodigoPartido(String codigoPartido);
}
