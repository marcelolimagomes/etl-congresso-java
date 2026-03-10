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
 * Entidade Silver — ocupações profissionais de deputados da Câmara.
 * Espelha o CSV deputadosOcupacoes.csv.
 * Chave de deduplicação: (id_deputado, titulo, ano_inicio, entidade).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_ocupacao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_ocupacao_nat_key", columnNames = {
        "id_deputado", "titulo", "ano_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoOcupacao {

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

    // ── Campos do CSV deputadosOcupacoes.csv ───────────────────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "titulo", length = 300)
    private String titulo;

    @Column(name = "ano_inicio", length = 10)
    private String anoInicio;

    @Column(name = "ano_fim", length = 10)
    private String anoFim;

    @Column(name = "entidade", length = 300)
    private String entidade;

    @Column(name = "entidade_uf", length = 5)
    private String entidadeUF;

    @Column(name = "entidade_pais", length = 100)
    private String entidadePais;
}
