package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoRelatoria;

@Repository
public interface SilverSenadoRelatoriaRepository extends JpaRepository<SilverSenadoRelatoria, UUID> {

    boolean existsBySenadoMateriaIdAndIdRelatoria(UUID senadoMateriaId, Long idRelatoria);
}
