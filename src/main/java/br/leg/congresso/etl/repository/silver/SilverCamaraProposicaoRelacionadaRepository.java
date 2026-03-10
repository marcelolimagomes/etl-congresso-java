package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoRelacionada;

@Repository
public interface SilverCamaraProposicaoRelacionadaRepository
        extends JpaRepository<SilverCamaraProposicaoRelacionada, UUID> {

    boolean existsByProposicaoIdAndRelacionadaId(String proposicaoId, Integer relacionadaId);
}
