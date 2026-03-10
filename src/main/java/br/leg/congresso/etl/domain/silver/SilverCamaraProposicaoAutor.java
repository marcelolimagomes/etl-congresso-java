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
 * Entidade Silver — autores de proposições da Câmara.
 * Espelha o CSV proposicoesAutores-{ano}.csv (12 colunas, verificado 2024).
 * Chave de deduplicação: (uri_proposicao, nome_autor, ordem_assinatura).
 */
@Entity
@Table(schema = "silver", name = "camara_proposicao_autor", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_proposicao_autor", columnNames = {
        "uri_proposicao", "nome_autor", "ordem_assinatura" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraProposicaoAutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camara_proposicao_id")
    private SilverCamaraProposicao camaraProposicao;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "CSV";

    // ── Campos do CSV proposicoesAutores-{ano}.csv ────────────────────────────
    @Column(name = "id_proposicao", length = 50)
    private String idProposicao;
    @Column(name = "uri_proposicao", length = 500)
    private String uriProposicao;
    @Column(name = "id_deputado_autor", length = 50)
    private String idDeputadoAutor;
    @Column(name = "uri_autor", length = 500)
    private String uriAutor;
    @Column(name = "cod_tipo_autor", length = 50)
    private String codTipoAutor;
    @Column(name = "tipo_autor", length = 100)
    private String tipoAutor;
    @Column(name = "nome_autor", length = 500)
    private String nomeAutor;
    @Column(name = "sigla_partido_autor", length = 20)
    private String siglaPartidoAutor;
    @Column(name = "uri_partido_autor", length = 500)
    private String uriPartidoAutor;
    @Column(name = "sigla_uf_autor", length = 5)
    private String siglaUfAutor;
    @Column(name = "ordem_assinatura")
    private Integer ordemAssinatura;
    @Column(name = "proponente")
    private Integer proponente;
}
