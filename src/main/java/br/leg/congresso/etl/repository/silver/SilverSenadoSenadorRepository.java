package br.leg.congresso.etl.repository.silver;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;

/**
 * Repositório Silver — senadores do Senado Federal.
 * Tabela: silver.senado_senador
 */
@Repository
public interface SilverSenadoSenadorRepository
        extends JpaRepository<SilverSenadoSenador, UUID> {

    /**
     * Verifica se já existe senador com o código fornecido.
     */
    boolean existsByCodigoSenador(String codigoSenador);

    /**
     * Busca senador pelo código para atualização dos campos det_*.
     */
    Optional<SilverSenadoSenador> findByCodigoSenador(String codigoSenador);
}
