package br.leg.congresso.etl.repository.silver;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacao;

@Repository
public interface SilverCamaraVotacaoRepository extends JpaRepository<SilverCamaraVotacao, UUID> {

    boolean existsByVotacaoId(String votacaoId);

    List<SilverCamaraVotacao> findAllByVotacaoIdIn(Collection<String> votacaoIds);

    @Query("SELECT COUNT(v) FROM SilverCamaraVotacao v WHERE v.data >= :dataInicio AND v.data <= :dataFim")
    long countByDataBetween(@Param("dataInicio") java.time.LocalDate dataInicio,
            @Param("dataFim") java.time.LocalDate dataFim);
}
