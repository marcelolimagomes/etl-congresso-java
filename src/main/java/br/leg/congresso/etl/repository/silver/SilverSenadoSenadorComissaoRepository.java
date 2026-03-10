package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorComissao;

/**
 * Repositório Silver — comissões por senador.
 * Tabela: silver.senado_senador_comissao
 */
@Repository
public interface SilverSenadoSenadorComissaoRepository
        extends JpaRepository<SilverSenadoSenadorComissao, UUID> {

    boolean existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
            String codigoSenador, String codigoComissao, String dataInicioParticipacao);
}
