package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoSituacao;

@Repository
public interface SilverSenadoRefTipoSituacaoRepository
        extends JpaRepository<SilverSenadoRefTipoSituacao, UUID> {
}
