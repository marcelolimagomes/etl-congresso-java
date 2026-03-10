package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidade Silver — participação de deputados em órgãos da Câmara.
 * Espelha o CSV orgaosDeputados-L{leg}.csv.
 * Chave de deduplicação: (id_deputado, id_orgao, data_inicio).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_orgao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_orgao_nat_key", columnNames = {
        "id_deputado", "id_orgao", "data_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoOrgao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "CSV";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos do CSV orgaosDeputados-L{leg}.csv ───────────────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "id_orgao", length = 20)
    private String idOrgao;

    @Column(name = "sigla_orgao", length = 30)
    private String siglaOrgao;

    @Column(name = "nome_orgao", length = 300)
    private String nomeOrgao;

    @Column(name = "nome_publicacao", length = 300)
    private String nomePublicacao;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "cod_titulo", length = 20)
    private String codTitulo;

    @Column(name = "data_inicio", length = 30)
    private String dataInicio;

    @Column(name = "data_fim", length = 30)
    private String dataFim;

    @Column(name = "uri_orgao", length = 500)
    private String uriOrgao;
}
