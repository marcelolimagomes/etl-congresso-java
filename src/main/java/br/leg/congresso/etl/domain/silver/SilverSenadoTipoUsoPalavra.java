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
 * Entidade Silver — tipos de uso da palavra no Senado Federal.
 * Fonte: API GET /senador/lista/tiposUsoPalavra.
 * Chave de deduplicação: (codigo_tipo).
 */
@Entity
@Table(schema = "silver", name = "senado_tipo_uso_palavra", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_tipo_uso_palavra_nat_key", columnNames = {
        "codigo_tipo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoTipoUsoPalavra {

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
    private String origemCarga = "API";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos da API /senador/lista/tiposUsoPalavra ───────────────────────────

    @Column(name = "codigo_tipo", length = 20)
    private String codigoTipo;

    @Column(name = "descricao_tipo", length = 200)
    private String descricaoTipo;

    @Column(name = "abreviatura", length = 20)
    private String abreviatura;
}
