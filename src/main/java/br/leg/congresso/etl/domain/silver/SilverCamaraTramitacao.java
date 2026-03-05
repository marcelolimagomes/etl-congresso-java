package br.leg.congresso.etl.domain.silver;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade Silver para tramitações da Câmara.
 * Espelha fielmente o payload de GET /api/v2/proposicoes/{id}/tramitacoes.
 */
@Entity
@Table(
    schema = "silver",
    name = "camara_tramitacao",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_silver_camara_tramitacao",
        columnNames = {"camara_proposicao_id", "sequencia"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraTramitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camara_proposicao_id", nullable = false)
    private SilverCamaraProposicao camaraProposicao;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // ── Campos do payload /tramitacoes ─────────────────────────────────────────

    @Column
    private Integer sequencia;

    @Column(name = "data_hora", length = 50)
    private String dataHora;

    @Column(name = "sigla_orgao", length = 50)
    private String siglaOrgao;

    @Column(name = "uri_orgao", length = 500)
    private String uriOrgao;

    @Column(name = "uri_ultimo_relator", length = 500)
    private String uriUltimoRelator;

    @Column(length = 200)
    private String regime;

    @Column(name = "descricao_tramitacao", columnDefinition = "TEXT")
    private String descricaoTramitacao;

    @Column(name = "cod_tipo_tramitacao", length = 20)
    private String codTipoTramitacao;

    @Column(name = "descricao_situacao", length = 500)
    private String descricaoSituacao;

    @Column(name = "cod_situacao")
    private Integer codSituacao;

    @Column(columnDefinition = "TEXT")
    private String despacho;

    @Column(length = 500)
    private String url;

    @Column(length = 200)
    private String ambito;

    @Column(length = 200)
    private String apreciacao;
}
