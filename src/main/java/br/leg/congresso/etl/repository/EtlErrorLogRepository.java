package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.EtlErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EtlErrorLogRepository extends JpaRepository<EtlErrorLog, UUID> {

    List<EtlErrorLog> findByJobIdOrderByCriadoEmDesc(UUID jobId);

    long countByJobId(UUID jobId);
}
