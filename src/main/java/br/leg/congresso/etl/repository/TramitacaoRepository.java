package br.leg.congresso.etl.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.leg.congresso.etl.domain.Tramitacao;

public interface TramitacaoRepository extends JpaRepository<Tramitacao, UUID> {

    @Modifying
    @Query("DELETE FROM Tramitacao t WHERE t.proposicao.id = :proposicaoId")
    void deleteByProposicaoId(UUID proposicaoId);

    long countByProposicaoId(UUID proposicaoId);

    @Query("SELECT t FROM Tramitacao t WHERE t.proposicao.id = :proposicaoId ORDER BY t.sequencia ASC NULLS LAST, t.dataHora ASC NULLS LAST")
    List<Tramitacao> findByProposicaoIdOrdered(@Param("proposicaoId") UUID proposicaoId);
}
