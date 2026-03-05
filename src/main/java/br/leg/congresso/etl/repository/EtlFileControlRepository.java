package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.EtlFileControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtlFileControlRepository extends JpaRepository<EtlFileControl, UUID> {

    Optional<EtlFileControl> findByOrigemAndNomeArquivo(CasaLegislativa origem, String nomeArquivo);

    List<EtlFileControl> findByOrigemAndAnoReferenciaAndStatus(
        CasaLegislativa origem, Integer ano, StatusEtl status
    );

    /** Marca arquivo para ser reprocessado independente do checksum */
    @Modifying
    @Query("UPDATE EtlFileControl f SET f.forcarReprocessamento = true WHERE f.origem = :origem AND f.anoReferencia = :ano")
    int marcarParaReprocessamento(@Param("origem") CasaLegislativa origem, @Param("ano") Integer ano);

    @Modifying
    @Query("UPDATE EtlFileControl f SET f.forcarReprocessamento = true WHERE f.origem = :origem AND f.dataReferencia = :data")
    int marcarParaReprocessamentoPorData(@Param("origem") CasaLegislativa origem, @Param("data") LocalDate data);
}
