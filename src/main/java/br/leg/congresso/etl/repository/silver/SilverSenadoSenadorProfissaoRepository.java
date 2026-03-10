package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorProfissao;

/**
 * Repositório Silver — profissões por senador.
 * Tabela: silver.senado_senador_profissao
 */
@Repository
public interface SilverSenadoSenadorProfissaoRepository
        extends JpaRepository<SilverSenadoSenadorProfissao, UUID> {

    boolean existsByCodigoSenadorAndCodigoProfissao(String codigoSenador, String codigoProfissao);
}
