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
 * Entidade Silver para relatorias de matérias do Senado.
 * Espelha fielmente o payload de GET
 * /dadosabertos/processo/relatoria?codigoMateria={codigo}.
 *
 * Princípio Silver: passthrough da fonte — sem normalização.
 * Deduplicação: chave composta (senado_materia_id, id_relatoria).
 */
@Entity
@Table(schema = "silver", name = "senado_relatoria", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_relatoria", columnNames = {
        "senado_materia_id", "id_relatoria" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverSenadoRelatoria {

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

    // ── Identificação da relatoria ────────────────────────────────────────────

    /** Campo {@code id} da API — identificador único da relatoria. */
    @Column(name = "id_relatoria", nullable = false)
    private Long idRelatoria;

    @Column(name = "casa_relator", length = 10)
    private String casaRelator;

    @Column(name = "id_tipo_relator")
    private Long idTipoRelator;

    @Column(name = "descricao_tipo_relator", length = 200)
    private String descricaoTipoRelator;

    @Column(name = "data_designacao", length = 20)
    private String dataDesignacao;

    @Column(name = "data_destituicao", length = 20)
    private String dataDestituicao;

    @Column(name = "descricao_tipo_encerramento", length = 300)
    private String descricaoTipoEncerramento;

    @Column(name = "id_processo")
    private Long idProcesso;

    @Column(name = "identificacao_processo", length = 100)
    private String identificacaoProcesso;

    @Column(name = "tramitando", length = 5)
    private String tramitando;

    // ── Campos do parlamentar relator ─────────────────────────────────────────

    @Column(name = "codigo_parlamentar")
    private Long codigoParlamentar;

    @Column(name = "nome_parlamentar", length = 300)
    private String nomeParlamentar;

    @Column(name = "nome_completo", length = 300)
    private String nomeCompleto;

    @Column(name = "sexo_parlamentar", length = 2)
    private String sexoParlamentar;

    @Column(name = "forma_tratamento", length = 100)
    private String formaTratamentoParlamentar;

    @Column(name = "sigla_partido", length = 20)
    private String siglaPartidoParlamentar;

    @Column(name = "uf_parlamentar", length = 5)
    private String ufParlamentar;

    // ── Campos do colegiado ───────────────────────────────────────────────────

    @Column(name = "codigo_colegiado")
    private Long codigoColegiado;

    @Column(name = "sigla_casa", length = 10)
    private String siglaCasa;

    @Column(name = "sigla_colegiado", length = 30)
    private String siglaColegiado;

    @Column(name = "nome_colegiado", length = 300)
    private String nomeColegiado;

    @Column(name = "codigo_tipo_colegiado")
    private Long codigoTipoColegiado;
}
