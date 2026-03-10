package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
 * Entidade Silver para prazos de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/processo/prazo?codigoMateria={codigo}.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: chave composta (senado_materia_id, tipo_prazo, data_inicio).
 */
@Entity
@Table(schema = "silver", name = "senado_prazo", uniqueConstraints = @UniqueConstraint(name = "uq_senado_prazo", columnNames = {
        "senado_materia_id", "tipo_prazo", "data_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoPrazo {

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

    // ── Campos do prazo ───────────────────────────────────────────────────────

    @Column(name = "tipo_prazo", length = 200)
    private String tipoPrazo;

    @Column(name = "data_inicio", length = 30)
    private String dataInicio;

    @Column(name = "data_fim", length = 30)
    private String dataFim;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "colegiado", length = 200)
    private String colegiado;

    @Column(name = "situacao", length = 100)
    private String situacao;
}
