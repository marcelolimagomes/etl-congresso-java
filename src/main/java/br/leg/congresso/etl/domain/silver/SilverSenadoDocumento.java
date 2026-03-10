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
 * Entidade Silver para documentos de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/processo/documento?codigoMateria={codigo}.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: chave composta (senado_materia_id, codigo_documento).
 */
@Entity
@Table(schema = "silver", name = "senado_documento", uniqueConstraints = @UniqueConstraint(name = "uq_senado_documento", columnNames = {
        "senado_materia_id", "codigo_documento" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoDocumento {

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

    // ── Campos do documento ───────────────────────────────────────────────────

    @Column(name = "codigo_documento", length = 50)
    private String codigoDocumento;

    @Column(name = "tipo_documento", length = 200)
    private String tipoDocumento;

    @Column(name = "descricao_tipo_documento", length = 500)
    private String descricaoTipoDocumento;

    @Column(name = "data_documento", length = 30)
    private String dataDocumento;

    @Column(name = "descricao_documento", columnDefinition = "TEXT")
    private String descricaoDocumento;

    @Column(name = "url_documento", length = 1000)
    private String urlDocumento;

    @Column(name = "tipo_conteudo", length = 100)
    private String tipoConteudo;

    @Column(name = "autor_nome", length = 500)
    private String autorNome;

    @Column(name = "autor_codigo_parlamentar", length = 50)
    private String autorCodigoParlamentar;
}
