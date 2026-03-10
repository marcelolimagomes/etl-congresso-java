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
 * Entidade Silver — partidos com representação no Senado Federal.
 * Fonte: API GET /senador/partidos.
 * Chave de deduplicação: (codigo_partido).
 */
@Entity
@Table(schema = "silver", name = "senado_partido", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_partido_nat_key", columnNames = {
        "codigo_partido" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoPartido {

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

    // ── Campos da API /senador/partidos ────────────────────────────────────────

    @Column(name = "codigo_partido", length = 20)
    private String codigoPartido;

    @Column(name = "sigla_partido", length = 20)
    private String siglaPartido;

    @Column(name = "nome_partido", length = 200)
    private String nomePartido;

    @Column(name = "data_ativacao", length = 30)
    private String dataAtivacao;

    @Column(name = "data_desativacao", length = 30)
    private String dataDesativacao;
}
