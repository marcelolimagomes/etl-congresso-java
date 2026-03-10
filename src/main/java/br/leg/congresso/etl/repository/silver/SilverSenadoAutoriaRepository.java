package br.leg.congresso.etl.repository.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;

@Repository
public interface SilverSenadoAutoriaRepository extends JpaRepository<SilverSenadoAutoria, UUID> {

    boolean existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
            UUID senadoMateriaId, String nomeAutor, String codigoTipoAutor);

    List<SilverSenadoAutoria> findBySenadoMateriaId(UUID senadoMateriaId);
}
