package br.leg.congresso.etl.domain.silver;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade Silver para movimentações do Senado.
 * Espelha fielmente o payload de GET /dadosabertos/materia/{id}/movimentacoes.json.
 */
@Entity
@Table(
    schema = "silver",
    name = "senado_movimentacao",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_silver_senado_movimentacao",
        columnNames = {"senado_materia_id", "sequencia_movimentacao"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senado_materia_id", nullable = false)
    private SilverSenadoMateria senadoMateria;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // ── Campos do payload /movimentacoes ──────────────────────────────────────

    @Column(name = "sequencia_movimentacao", length = 20)
    private String sequenciaMovimentacao;

    @Column(name = "data_movimentacao", length = 30)
    private String dataMovimentacao;

    @Column(name = "identificacao_tramitacao", length = 30)
    private String identificacaoTramitacao;

    @Column(name = "descricao_movimentacao", columnDefinition = "TEXT")
    private String descricaoMovimentacao;

    @Column(name = "descricao_situacao", length = 500)
    private String descricaoSituacao;

    @Column(columnDefinition = "TEXT")
    private String despacho;

    @Column(length = 200)
    private String ambito;

    @Column(name = "sigla_local", length = 100)
    private String siglaLocal;

    @Column(name = "nome_local", length = 500)
    private String nomeLocal;
}
