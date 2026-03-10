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
 * Entidade Silver para emendas de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/processo/emenda?codigoMateria={codigo}.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: chave composta (senado_materia_id, codigo_emenda).
 */
@Entity
@Table(schema = "silver", name = "senado_emenda", uniqueConstraints = @UniqueConstraint(name = "uq_senado_emenda", columnNames = {
        "senado_materia_id", "codigo_emenda" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoEmenda {

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

    // ── Campos da emenda ──────────────────────────────────────────────────────

    @Column(name = "codigo_emenda", length = 50)
    private String codigoEmenda;

    @Column(name = "tipo_emenda", length = 100)
    private String tipoEmenda;

    @Column(name = "descricao_tipo_emenda", length = 200)
    private String descricaoTipoEmenda;

    @Column(name = "numero_emenda", length = 50)
    private String numeroEmenda;

    @Column(name = "data_apresentacao", length = 30)
    private String dataApresentacao;

    @Column(name = "colegiado_apresentacao", length = 200)
    private String colegiadoApresentacao;

    @Column(name = "turno", length = 50)
    private String turno;

    @Column(name = "autor_nome", length = 500)
    private String autorNome;

    @Column(name = "autor_codigo_parlamentar", length = 50)
    private String autorCodigoParlamentar;

    @Column(name = "autor_tipo", length = 100)
    private String autorTipo;

    @Column(name = "ementa", columnDefinition = "TEXT")
    private String ementa;

    @Column(name = "inteiro_teor_url", length = 1000)
    private String inteiroTeorUrl;
}
