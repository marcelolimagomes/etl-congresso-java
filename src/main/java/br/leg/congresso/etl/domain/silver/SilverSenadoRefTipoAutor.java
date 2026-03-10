package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabela de referência: tipos de autor em Processos Legislativos.
 * Fonte: GET /dadosabertos/processo/tipos-autor
 * Estratégia: Full replace (truncate + insert) — dados estáticos.
 */
@Entity
@Table(schema = "silver", name = "senado_ref_tipo_autor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoRefTipoAutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "descricao", length = 500)
    private String descricao;
}
