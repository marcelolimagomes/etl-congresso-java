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
 * Entidade Silver — proposições relacionadas da Câmara dos Deputados.
 * Espelha a resposta de GET /api/v2/proposicoes/{id}/relacionadas.
 * Chave de deduplicação: (proposicao_id, relacionada_id).
 */
@Entity
@Table(schema = "silver", name = "camara_proposicao_relacionada", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_proposicao_relacionada", columnNames = {
        "proposicao_id", "relacionada_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraProposicaoRelacionada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID etlJobId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime ingeridoEm;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String origemCarga = "API";

    // ── ID da proposição de origem ────────────────────────────────────────────

    @Column(nullable = false, length = 100)
    private String proposicaoId;

    // ── Campos da proposição relacionada (passthrough da API) ─────────────────

    @Column(nullable = false)
    private Integer relacionadaId;

    @Column(length = 500)
    private String relacionadaUri;

    @Column(length = 20)
    private String relacionadaSiglaTipo;

    @Column(length = 20)
    private String relacionadaNumero;

    @Column(length = 10)
    private String relacionadaAno;

    @Column(columnDefinition = "text")
    private String relacionadaEmenta;

    @Column(length = 20)
    private String relacionadaCodTipo;

    // ── FK opcional para silver.camara_proposicao ─────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camara_proposicao_id")
    private SilverCamaraProposicao camaraProposicao;
}
