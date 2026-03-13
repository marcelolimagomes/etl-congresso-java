package br.leg.congresso.etl.repository.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoRelatoria;

@Repository
public interface SilverSenadoRelatoriaRepository extends JpaRepository<SilverSenadoRelatoria, UUID> {

    boolean existsBySenadoMateriaIdAndIdRelatoria(UUID senadoMateriaId, Long idRelatoria);

    @Query("""
            select r from SilverSenadoRelatoria r
            join fetch r.senadoMateria m
            where r.codigoParlamentar = :codigoParlamentar
            order by m.ano desc, m.numero desc, r.idRelatoria desc
            """)
    List<SilverSenadoRelatoria> findByCodigoParlamentarOrderByMateriaDesc(
            @Param("codigoParlamentar") Long codigoParlamentar);
}
