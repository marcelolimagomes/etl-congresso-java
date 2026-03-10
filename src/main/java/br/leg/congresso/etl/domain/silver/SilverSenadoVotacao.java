package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade Silver para votações de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/votacao?codigoMateria={codigo}.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: (senado_materia_id, codigo_sessao_votacao, sequencial_sessao).
 */
@Entity
@Table(schema = "silver", name = "senado_votacao", uniqueConstraints = @UniqueConstraint(name = "uq_senado_votacao", columnNames = {
        "senado_materia_id", "codigo_sessao_votacao", "sequencial_sessao" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senado_materia_id")
    private SilverSenadoMateria senadoMateria;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "API";

    @Column(name = "codigo_materia", length = 50)
    private String codigoMateria;

    // ── Campos da votação ─────────────────────────────────────────────────────

    @Column(name = "codigo_sessao", length = 50)
    private String codigoSessao;

    @Column(name = "sigla_casa", length = 10)
    private String siglaCasa;

    @Column(name = "codigo_sessao_votacao", length = 50)
    private String codigoSessaoVotacao;

    @Column(name = "sequencial_sessao", length = 50)
    private String sequencialSessao;

    @Column(name = "data_sessao", length = 30)
    private String dataSessao;

    @Column(name = "descricao_votacao", columnDefinition = "TEXT")
    private String descricaoVotacao;

    @Column(name = "resultado", length = 200)
    private String resultado;

    @Column(name = "descricao_resultado", columnDefinition = "TEXT")
    private String descricaoResultado;

    @Column(name = "total_votos_sim")
    private Integer totalVotosSim;

    @Column(name = "total_votos_nao")
    private Integer totalVotosNao;

    @Column(name = "total_votos_abstencao")
    private Integer totalVotosAbstencao;

    @Column(name = "indicador_votacao_secreta", length = 10)
    private String indicadorVotacaoSecreta;

    /** Votos dos parlamentares em formato JSONB (passthrough fiel). */
    @Column(name = "votos_parlamentares", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String votosParlamentares;
}
