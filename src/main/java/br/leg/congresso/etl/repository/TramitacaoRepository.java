package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.Tramitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TramitacaoRepository extends JpaRepository<Tramitacao, UUID> {

    @Modifying
    @Query("DELETE FROM Tramitacao t WHERE t.proposicao.id = :proposicaoId")
    void deleteByProposicaoId(UUID proposicaoId);

    long countByProposicaoId(UUID proposicaoId);
}
