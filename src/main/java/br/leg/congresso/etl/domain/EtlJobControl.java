package br.leg.congresso.etl.domain;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Rastreia cada execução do pipeline ETL.
 * Permite auditoria, métricas e diagnóstico.
 */
@Entity
@Table(name = "etl_job_control", indexes = {
    @Index(name = "idx_job_control_origem",   columnList = "origem, tipo_execucao"),
    @Index(name = "idx_job_control_status",   columnList = "status"),
    @Index(name = "idx_job_control_iniciado", columnList = "iniciado_em")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtlJobControl {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CasaLegislativa origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_execucao", length = 20, nullable = false)
    private TipoExecucao tipoExecucao;

    @CreationTimestamp
    @Column(name = "iniciado_em", updatable = false)
    private LocalDateTime iniciadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "total_processado", nullable = false)
    @Builder.Default
    private int totalProcessado = 0;

    @Column(name = "total_inserido", nullable = false)
    @Builder.Default
    private int totalInserido = 0;

    @Column(name = "total_atualizado", nullable = false)
    @Builder.Default
    private int totalAtualizado = 0;

    @Column(name = "total_ignorados", nullable = false)
    @Builder.Default
    private int totalIgnorados = 0;

    @Column(name = "total_erros", nullable = false)
    @Builder.Default
    private int totalErros = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private StatusEtl status = StatusEtl.RUNNING;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    /** Parâmetros de execução: anos, datas, etc. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> parametros;

    // ── Métodos utilitários ──────────────────────────────────────────

    public void incrementarProcessado() { totalProcessado++; }
    public void incrementarInserido()   { totalInserido++;   }
    public void incrementarAtualizado() { totalAtualizado++; }
    public void incrementarIgnorados()  { totalIgnorados++;  }
    public void incrementarErros()      { totalErros++;      }

    public void finalizar(StatusEtl statusFinal) {
        this.status = statusFinal;
        this.finalizadoEm = LocalDateTime.now();
    }
}
