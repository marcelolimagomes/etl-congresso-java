package br.leg.congresso.etl.domain.silver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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

/**
 * Entidade Silver — votações da Câmara dos Deputados.
 * Espelha o CSV votacoes-{ano}.csv (20 colunas, verificado 2024).
 * Chave de deduplicação: votacao_id.
 */
@Entity
@Table(schema = "silver", name = "camara_votacao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_votacao", columnNames = {
        "votacao_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID etlJobId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime ingeridoEm;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String origemCarga = "CSV";

    // ── Campos do CSV votacoes-{ano}.csv ──────────────────────────────────────

    @Column(nullable = false, length = 60)
    private String votacaoId;

    @Column(columnDefinition = "text")
    private String uri;

    private LocalDate data;

    private OffsetDateTime dataHoraRegistro;

    private Integer idOrgao;

    @Column(columnDefinition = "text")
    private String uriOrgao;

    @Column(length = 20)
    private String siglaOrgao;

    private Integer idEvento;

    @Column(columnDefinition = "text")
    private String uriEvento;

    private Short aprovacao;

    private Integer votosSim;

    private Integer votosNao;

    private Integer votosOutros;

    @Column(columnDefinition = "text")
    private String descricao;

    // ── ultimaAberturaVotacao_ ────────────────────────────────────────────────

    private OffsetDateTime ultimaAberturaVotacaoDataHoraRegistro;

    @Column(columnDefinition = "text")
    private String ultimaAberturaVotacaoDescricao;

    // ── ultimaApresentacaoProposicao_ ─────────────────────────────────────────

    private OffsetDateTime ultimaApresentacaoProposicaoDataHoraRegistro;

    @Column(columnDefinition = "text")
    private String ultimaApresentacaoProposicaoDescricao;

    private Integer ultimaApresentacaoProposicaoIdProposicao;

    @Column(columnDefinition = "text")
    private String ultimaApresentacaoProposicaoUriProposicao;
}
