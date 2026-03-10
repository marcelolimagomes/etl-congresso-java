package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoPrazo;

@Repository
public interface SilverSenadoPrazoRepository extends JpaRepository<SilverSenadoPrazo, UUID> {

    boolean existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(
            UUID senadoMateriaId, String tipoPrazo, String dataInicio);
}
