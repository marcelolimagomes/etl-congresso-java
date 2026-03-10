package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAfastado;

/**
 * Repositório Silver — senadores afastados do Senado Federal.
 * Tabela: silver.senado_senador_afastado
 */
@Repository
public interface SilverSenadoSenadorAfastadoRepository
        extends JpaRepository<SilverSenadoSenadorAfastado, UUID> {

    /**
     * Verifica se já existe registro de afastamento com a chave natural composta.
     *
     * @param codigoSenador   código do senador
     * @param dataAfastamento data do afastamento
     */
    boolean existsByCodigoSenadorAndDataAfastamento(String codigoSenador, String dataAfastamento);
}
