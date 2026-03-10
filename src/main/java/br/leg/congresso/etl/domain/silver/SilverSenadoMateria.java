package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidade Silver para matérias do Senado.
 * Combina campos da pesquisa/lista com os campos do endpoint de detalhe.
 *
 * Princípio Silver: dados persistidos exatamente como vieram da API — sem
 * normalização.
 */
@Entity
@Table(schema = "silver", name = "senado_materia", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_materia_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "movimentacoes")
public class SilverSenadoMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** 'PESQUISA' | 'DETALHE' | 'INCREMENTAL' */
    @Column(name = "origem_carga", length = 30, nullable = false)
    @Builder.Default
    private String origemCarga = "PESQUISA";

    /**
     * Flag de controle: FALSE = pendente promoção para Gold; TRUE = já promovido
     */
    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos de pesquisa/lista.json ─────────────────────────────────────────

    /** Código único da matéria na API do Senado (campo "Codigo") */
    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(name = "identificacao_processo", length = 30)
    private String identificacaoProcesso;

    @Column(name = "descricao_identificacao", length = 100)
    private String descricaoIdentificacao;

    @Column(length = 20)
    private String sigla;

    /** Número com zeros à esquerda, preservado como vem da fonte (ex: "00001") */
    @Column(length = 20)
    private String numero;

    @Column
    private Integer ano;

    @Column(columnDefinition = "TEXT")
    private String ementa;

    @Column(length = 500)
    private String autor;

    /** Data de apresentação como vem da fonte */
    @Column(length = 30)
    private String data;

    @Column(name = "url_detalhe_materia", length = 500)
    private String urlDetalheMateria;

    // ── Campos do endpoint de detalhe (/materia/{id}.json) ────────────────────

    @Column(name = "det_sigla_casa_identificacao", length = 10)
    private String detSiglaCasaIdentificacao;

    @Column(name = "det_sigla_subtipo", length = 20)
    private String detSiglaSubtipo;

    @Column(name = "det_descricao_subtipo", length = 200)
    private String detDescricaoSubtipo;

    @Column(name = "det_descricao_objetivo_processo", length = 200)
    private String detDescricaoObjetivoProcesso;

    /** "Sim" / "Não" */
    @Column(name = "det_indicador_tramitando", length = 10)
    private String detIndicadorTramitando;

    @Column(name = "det_indexacao", columnDefinition = "TEXT")
    private String detIndexacao;

    @Column(name = "det_casa_iniciadora", length = 100)
    private String detCasaIniciadora;

    @Column(name = "det_indicador_complementar", length = 10)
    private String detIndicadorComplementar;

    @Column(name = "det_natureza_codigo", length = 20)
    private String detNaturezaCodigo;

    @Column(name = "det_natureza_nome", length = 100)
    private String detNaturezaNome;

    @Column(name = "det_natureza_descricao", length = 200)
    private String detNaturezaDescricao;

    @Column(name = "det_sigla_casa_origem", length = 10)
    private String detSiglaCasaOrigem;

    /** Array de classificações temáticas (JSONB) — mantido fiel ao array da API */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "det_classificacoes", columnDefinition = "jsonb")
    private String detClassificacoes;

    /** URLs de serviço (JSONB) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "det_outras_informacoes", columnDefinition = "jsonb")
    private String detOutrasInformacoes;

    // ── Dados do endpoint /textos/{codigo}.json ───────────────────────────────

    @Column(name = "url_texto", length = 1000)
    private String urlTexto;

    @Column(name = "data_texto", length = 30)
    private String dataTexto;

    @OneToMany(mappedBy = "senadoMateria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SilverSenadoMovimentacao> movimentacoes = new ArrayList<>();
}
