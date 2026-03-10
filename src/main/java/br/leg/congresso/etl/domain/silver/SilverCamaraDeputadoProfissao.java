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
 * Entidade Silver — profissões declaradas de deputados da Câmara.
 * Espelha o CSV deputadosProfissoes.csv.
 * Chave de deduplicação: (id_deputado, titulo).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_profissao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_profissao_nat_key", columnNames = {
        "id_deputado", "titulo", "cod_tipo_profissao" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoProfissao {

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

    // ── Campos do CSV deputadosProfissoes.csv ──────────────────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "titulo", length = 300)
    private String titulo;

    @Column(name = "cod_tipo_profissao", length = 20)
    private String codTipoProfissao;

    @Column(name = "data_hora", length = 30)
    private String dataHora;
}
