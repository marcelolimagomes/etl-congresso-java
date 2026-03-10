package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorFiliacao;

/**
 * Repositório Silver — filiações por senador.
 * Tabela: silver.senado_senador_filiacao
 */
@Repository
public interface SilverSenadoSenadorFiliacaoRepository
        extends JpaRepository<SilverSenadoSenadorFiliacao, UUID> {

    boolean existsByCodigoSenadorAndCodigoFiliacao(String codigoSenador, String codigoFiliacao);
}
