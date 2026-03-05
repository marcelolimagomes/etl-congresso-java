package br.leg.congresso.etl.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lock distribuído por recurso/ano para evitar execução paralela do mesmo conjunto.
 * Ex: recurso = "camara_2024" impede dois workers processando o mesmo ano ao mesmo tempo.
 */
@Entity
@Table(name = "etl_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtlLock {

    /** Identificador do recurso, ex: "camara_2024", "senado_full" */
    @Id
    @Column(length = 100)
    private String recurso;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    /** Hostname/instância para diagnóstico */
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
