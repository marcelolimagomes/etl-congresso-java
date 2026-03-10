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
 * Entidade Silver — composição da Mesa Diretora da Câmara.
 * Espelha o CSV legislaturasMesas.csv.
 * Chave de deduplicação: (id_deputado, titulo, id_legislatura).
 */
@Entity
@Table(schema = "silver", name = "camara_mesa_diretora", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_mesa_diretora_nat_key", columnNames = {
        "id_deputado", "id_legislatura", "titulo", "data_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraMesaDiretora {

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

    // ── Campos do CSV legislaturasMesas.csv ────────────────────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "id_legislatura", length = 10)
    private String idLegislatura;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "data_inicio", length = 30)
    private String dataInicio;

    @Column(name = "data_fim", length = 30)
    private String dataFim;
}
