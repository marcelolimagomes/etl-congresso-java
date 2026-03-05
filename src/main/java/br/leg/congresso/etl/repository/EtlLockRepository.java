package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.EtlLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EtlLockRepository extends JpaRepository<EtlLock, String> {

    @Modifying
    @Query(value = """
        INSERT INTO etl_lock (recurso, locked_at, locked_by, expires_at)
        VALUES (:recurso, :agora, :lockedBy, :expiresAt)
        ON CONFLICT (recurso) DO NOTHING
        """, nativeQuery = true)
    int tryAcquire(@Param("recurso") String recurso,
                   @Param("agora") LocalDateTime agora,
                   @Param("lockedBy") String lockedBy,
                   @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query(value = """
        UPDATE etl_lock
           SET locked_at = :agora,
               locked_by = :lockedBy,
               expires_at = :expiresAt
         WHERE recurso = :recurso
           AND expires_at < :agora
        """, nativeQuery = true)
    int tryAcquireExpired(@Param("recurso") String recurso,
                          @Param("agora") LocalDateTime agora,
                          @Param("lockedBy") String lockedBy,
                          @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query(value = "DELETE FROM etl_lock WHERE recurso = :recurso AND locked_by = :lockedBy", nativeQuery = true)
    int releaseOwned(@Param("recurso") String recurso,
                     @Param("lockedBy") String lockedBy);

    /** Remove locks expirados */
    @Modifying
    @Query("DELETE FROM EtlLock l WHERE l.expiresAt < :agora")
    int deleteExpired(@Param("agora") LocalDateTime agora);
}
