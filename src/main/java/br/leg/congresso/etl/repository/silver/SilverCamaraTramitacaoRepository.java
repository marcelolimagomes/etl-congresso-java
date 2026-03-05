package br.leg.congresso.etl.repository.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraTramitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SilverCamaraTramitacaoRepository extends JpaRepository<SilverCamaraTramitacao, UUID> {

    List<SilverCamaraTramitacao> findByCamaraProposicaoId(UUID camaraProposicaoId);

    boolean existsByCamaraProposicaoIdAndSequencia(UUID camaraProposicaoId, Integer sequencia);

    @Query("SELECT COUNT(t) FROM SilverCamaraTramitacao t WHERE t.camaraProposicao.ano = :ano")
    long countByProposicaoAno(@Param("ano") Integer ano);
}
