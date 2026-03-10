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
 * Entidade Silver — mandatos externos de deputados da Câmara.
 * Fonte: API GET /deputados/{id}/mandatosExternos.
 * Chave de deduplicação: (camara_deputado_id, cargo, sigla_uf, ano_inicio).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_mandato_externo", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_dep_mandato_ext_nat_key", columnNames = {
        "camara_deputado_id", "cargo", "sigla_uf", "ano_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoMandatoExterno {

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

    // ── Campos da API /deputados/{id}/mandatosExternos ─────────────────────────
    @Column(name = "camara_deputado_id", length = 20)
    private String camaraDeputadoId;

    @Column(name = "ano_inicio", length = 10)
    private String anoInicio;

    @Column(name = "ano_fim", length = 10)
    private String anoFim;

    @Column(name = "cargo", length = 200)
    private String cargo;

    @Column(name = "sigla_uf", length = 5)
    private String siglaUf;

    @Column(name = "municipio", length = 200)
    private String municipio;

    @Column(name = "sigla_partido_eleicao", length = 20)
    private String siglaPartidoEleicao;

    @Column(name = "uri_partido_eleicao", length = 500)
    private String uriPartidoEleicao;
}
