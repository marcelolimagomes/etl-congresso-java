package br.leg.congresso.etl.domain.silver;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade Silver para proposições da Câmara dos Deputados.
 * Espelha fielmente o CSV (31 campos) + complemento do endpoint de detalhe (GET /api/v2/proposicoes/{id}).
 *
 * Princípio Silver: sem transformações — os dados ficam exatamente como vieram da fonte.
 */
@Entity
@Table(
    schema = "silver",
    name = "camara_proposicao",
    uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_proposicao_camara_id", columnNames = "camara_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "tramitacoes")
public class SilverCamaraProposicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** ID do job ETL que gerou este registro */
    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** SHA-256 dos campos fonte — usado para deduplicação */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** Origem da carga: 'CSV' (full-load) ou 'API' (incremental) */
    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "CSV";

    /** Flag de controle: FALSE = pendente promoção para Gold; TRUE = já promovido */
    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos do CSV (colunas originais) ─────────────────────────────────────

    /** Identificador numérico da proposição na Câmara (coluna "id" do CSV) */
    @Column(name = "camara_id", length = 20)
    private String camaraId;

    @Column(length = 500)
    private String uri;

    @Column(name = "sigla_tipo", length = 20)
    private String siglaTipo;

    @Column(name = "cod_tipo")
    private Integer codTipo;

    @Column(name = "descricao_tipo", length = 200)
    private String descricaoTipo;

    @Column
    private Integer numero;

    @Column
    private Integer ano;

    @Column(columnDefinition = "TEXT")
    private String ementa;

    @Column(name = "ementa_detalhada", columnDefinition = "TEXT")
    private String ementaDetalhada;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    /** Data de apresentação como vem da fonte — sem parse/transformação */
    @Column(name = "data_apresentacao", length = 50)
    private String dataApresentacao;

    @Column(name = "uri_orgao_numerador", length = 500)
    private String uriOrgaoNumerador;

    @Column(name = "uri_prop_anterior", length = 500)
    private String uriPropAnterior;

    @Column(name = "uri_prop_principal", length = 500)
    private String uriPropPrincipal;

    @Column(name = "uri_prop_posterior", length = 500)
    private String uriPropPosterior;

    @Column(name = "url_inteiro_teor", length = 1000)
    private String urlInteiroTeor;

    @Column(name = "urn_final", length = 500)
    private String urnFinal;

    // ── Último status (prefixo ultimoStatus_*) ────────────────────────────────

    @Column(name = "ultimo_status_data_hora", length = 50)
    private String ultimoStatusDataHora;

    @Column(name = "ultimo_status_sequencia")
    private Integer ultimoStatusSequencia;

    @Column(name = "ultimo_status_uri_relator", length = 500)
    private String ultimoStatusUriRelator;

    @Column(name = "ultimo_status_id_orgao")
    private Integer ultimoStatusIdOrgao;

    @Column(name = "ultimo_status_sigla_orgao", length = 50)
    private String ultimoStatusSiglaOrgao;

    @Column(name = "ultimo_status_uri_orgao", length = 500)
    private String ultimoStatusUriOrgao;

    @Column(name = "ultimo_status_regime", length = 200)
    private String ultimoStatusRegime;

    @Column(name = "ultimo_status_descricao_tramitacao", columnDefinition = "TEXT")
    private String ultimoStatusDescricaoTramitacao;

    @Column(name = "ultimo_status_id_tipo_tramitacao", length = 20)
    private String ultimoStatusIdTipoTramitacao;

    @Column(name = "ultimo_status_descricao_situacao", length = 500)
    private String ultimoStatusDescricaoSituacao;

    @Column(name = "ultimo_status_id_situacao")
    private Integer ultimoStatusIdSituacao;

    @Column(name = "ultimo_status_despacho", columnDefinition = "TEXT")
    private String ultimoStatusDespacho;

    @Column(name = "ultimo_status_apreciacao", length = 200)
    private String ultimoStatusApreciacao;

    @Column(name = "ultimo_status_url", length = 500)
    private String ultimoStatusUrl;

    // ── Campos complementares do endpoint de detalhe (API) ────────────────────
    // GET /api/v2/proposicoes/{id} → campo statusProposicao

    @Column(name = "status_data_hora", length = 50)
    private String statusDataHora;

    @Column(name = "status_sequencia")
    private Integer statusSequencia;

    @Column(name = "status_sigla_orgao", length = 50)
    private String statusSiglaOrgao;

    @Column(name = "status_uri_orgao", length = 500)
    private String statusUriOrgao;

    @Column(name = "status_uri_ultimo_relator", length = 500)
    private String statusUriUltimoRelator;

    @Column(name = "status_regime", length = 200)
    private String statusRegime;

    @Column(name = "status_descricao_tramitacao", columnDefinition = "TEXT")
    private String statusDescricaoTramitacao;

    @Column(name = "status_cod_tipo_tramitacao", length = 20)
    private String statusCodTipoTramitacao;

    @Column(name = "status_descricao_situacao", length = 500)
    private String statusDescricaoSituacao;

    @Column(name = "status_cod_situacao")
    private Integer statusCodSituacao;

    @Column(name = "status_despacho", columnDefinition = "TEXT")
    private String statusDespacho;

    @Column(name = "status_url", length = 500)
    private String statusUrl;

    @Column(name = "status_ambito", length = 200)
    private String statusAmbito;

    @Column(name = "status_apreciacao", length = 200)
    private String statusApreciacao;

    @Column(name = "uri_autores", length = 500)
    private String uriAutores;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @OneToMany(mappedBy = "camaraProposicao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SilverCamaraTramitacao> tramitacoes = new ArrayList<>();
}
