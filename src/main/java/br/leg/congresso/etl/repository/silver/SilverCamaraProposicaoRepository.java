package br.leg.congresso.etl.repository.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SilverCamaraProposicaoRepository extends JpaRepository<SilverCamaraProposicao, UUID> {

    Optional<SilverCamaraProposicao> findByCamaraId(String camaraId);

    List<SilverCamaraProposicao> findAllByCamaraIdIn(Collection<String> camaraIds);

    List<SilverCamaraProposicao> findByGoldSincronizadoFalse();

    @Query("SELECT COUNT(s) FROM SilverCamaraProposicao s WHERE s.ano = :ano")
    long countByAno(@Param("ano") Integer ano);

    @Query("SELECT COUNT(s) FROM SilverCamaraProposicao s WHERE s.goldSincronizado = false")
    long countPendentesPromocao();

    @Modifying
    @Query("UPDATE SilverCamaraProposicao s SET s.goldSincronizado = true WHERE s.id = :id")
    void marcarGoldSincronizado(@Param("id") UUID id);
}
