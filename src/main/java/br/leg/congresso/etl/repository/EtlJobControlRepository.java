package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtlJobControlRepository extends JpaRepository<EtlJobControl, UUID> {

    List<EtlJobControl> findByStatusOrderByIniciadoEmDesc(StatusEtl status);

    Optional<EtlJobControl> findFirstByOrigemAndStatusOrderByIniciadoEmDesc(
        CasaLegislativa origem, StatusEtl status
    );

    @Query("SELECT j FROM EtlJobControl j ORDER BY j.iniciadoEm DESC")
    List<EtlJobControl> findRecentes(Pageable pageable);

    boolean existsByOrigemAndStatus(CasaLegislativa origem, StatusEtl status);
}
