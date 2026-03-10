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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade Silver — orientações de bancada para votações da Câmara.
 * Espelha o CSV votacoesOrientacoes-{ano}.csv (7 colunas, verificado 2024).
 * Chave de deduplicação: (id_votacao, sigla_bancada).
 */
@Entity
@Table(schema = "silver", name = "camara_votacao_orientacao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_votacao_orientacao", columnNames = {
        "id_votacao", "sigla_bancada" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraVotacaoOrientacao {

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

    // ── Campos do CSV votacoesOrientacoes-{ano}.csv ───────────────────────────

    @Column(nullable = false, length = 60)
    private String idVotacao;

    @Column(columnDefinition = "text")
    private String uriVotacao;

    @Column(length = 20)
    private String siglaOrgao;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(length = 60)
    private String siglaBancada;

    @Column(columnDefinition = "text")
    private String uriBancada;

    @Column(length = 30)
    private String orientacao;
}
