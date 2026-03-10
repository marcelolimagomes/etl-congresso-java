package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoTipoUsoPalavra;

/**
 * Repositório Silver — tipos de uso da palavra no Senado Federal.
 * Tabela: silver.senado_tipo_uso_palavra
 */
@Repository
public interface SilverSenadoTipoUsoPalavraRepository
        extends JpaRepository<SilverSenadoTipoUsoPalavra, UUID> {

    /**
     * Verifica se já existe tipo de uso da palavra com o código fornecido.
     */
    boolean existsByCodigoTipo(String codigoTipo);
}
