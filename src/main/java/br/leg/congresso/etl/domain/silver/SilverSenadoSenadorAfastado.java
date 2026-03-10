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
 * Entidade Silver — senadores afastados do Senado Federal.
 * Fonte: API GET /senador/afastados.
 * Chave de deduplicação: (codigo_senador, data_afastamento).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_afastado", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_senador_afastado_nat_key", columnNames = {
        "codigo_senador", "data_afastamento" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorAfastado {

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

    // ── Campos da API /senador/afastados ───────────────────────────────────────

    @Column(name = "codigo_senador", length = 20)
    private String codigoSenador;

    @Column(name = "nome_parlamentar", length = 300)
    private String nomeParlamentar;

    @Column(name = "uf_mandato", length = 5)
    private String ufMandato;

    @Column(name = "motivo_afastamento", length = 300)
    private String motivoAfastamento;

    @Column(name = "data_afastamento", length = 30)
    private String dataAfastamento;

    @Column(name = "data_termino_afastamento", length = 30)
    private String dataTerminoAfastamento;
}
