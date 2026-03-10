package br.leg.congresso.etl.repository.silver;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;

@Repository
public interface SilverCamaraDeputadoRepository extends JpaRepository<SilverCamaraDeputado, UUID> {

    List<SilverCamaraDeputado> findAllByCamaraIdIn(Collection<String> camaraIds);

    List<SilverCamaraDeputado> findByGoldSincronizadoFalse();

    /**
     * Retorna todos os deputados que ainda não foram enriquecidos via GET
     * /deputados/{id}
     */
    List<SilverCamaraDeputado> findByDetStatusIdIsNull();

    @Query("SELECT COUNT(d) FROM SilverCamaraDeputado d WHERE d.goldSincronizado = false")
    long countPendentesPromocao();

    @Modifying
    @Query("UPDATE SilverCamaraDeputado d SET d.goldSincronizado = true WHERE d.id = :id")
    void marcarGoldSincronizado(@Param("id") UUID id);
}
