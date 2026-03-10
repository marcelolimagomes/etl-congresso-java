package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidade Silver — participação de deputados em frentes parlamentares.
 * Espelha o CSV frentesDeputados.csv.
 * Chave de deduplicação: (id_deputado, id_frente).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_frente", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_frente_nat_key", columnNames = {
        "id_deputado", "id_frente" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoFrente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "CSV";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos do CSV frentesDeputados.csv ─────────────────────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "id_frente", length = 20)
    private String idFrente;

    @Column(name = "id_legislatura", length = 10)
    private String idLegislatura;

    @Column(name = "titulo", length = 500)
    private String titulo;

    @Column(name = "uri", length = 500)
    private String uri;
}
