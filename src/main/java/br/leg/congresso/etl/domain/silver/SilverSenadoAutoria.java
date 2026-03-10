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
 * Entidade Silver para autoria de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/materia/autoria/{codigo}.json.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: chave composta (senado_materia_id, nome_autor,
 * codigo_tipo_autor).
 */
@Entity
@Table(schema = "silver", name = "senado_autoria", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_autoria", columnNames = {
        "senado_materia_id", "nome_autor", "codigo_tipo_autor" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoAutoria {

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

    // ── Campos de autoria ─────────────────────────────────────────────────────

    @Column(name = "nome_autor", length = 300, nullable = false)
    private String nomeAutor;

    @Column(name = "sexo_autor", length = 2)
    private String sexoAutor;

    @Column(name = "codigo_tipo_autor", length = 50, nullable = false)
    private String codigoTipoAutor;

    @Column(name = "descricao_tipo_autor", length = 300)
    private String descricaoTipoAutor;

    // ── Campos do parlamentar (null quando o autor é entidade, não parlamentar) ─

    @Column(name = "codigo_parlamentar", length = 20)
    private String codigoParlamentar;

    @Column(name = "nome_parlamentar", length = 300)
    private String nomeParlamentar;

    @Column(name = "sigla_partido", length = 20)
    private String siglaPartido;

    @Column(name = "uf_parlamentar", length = 5)
    private String ufParlamentar;
}
