package br.leg.congresso.etl.domain.silver;

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
 * Entidade Silver — votos individuais de deputados em votações da Câmara.
 * Espelha o CSV votacoesVotos-{ano}.csv (12 colunas, verificado 2024).
 * Chave de deduplicação: (id_votacao, deputado_id).
 */
@Entity
@Table(schema = "silver", name = "camara_votacao_voto", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_votacao_voto", columnNames = {
        "id_votacao", "deputado_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverCamaraVotacaoVoto {

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

    // ── Campos do CSV votacoesVotos-{ano}.csv ─────────────────────────────────

    @Column(nullable = false, length = 60)
    private String idVotacao;

    @Column(columnDefinition = "text")
    private String uriVotacao;

    private OffsetDateTime dataHoraVoto;

    @Column(length = 30)
    private String voto;

    // ── Prefixo deputado_ ─────────────────────────────────────────────────────

    @Column(nullable = false)
    private Integer deputadoId;

    @Column(columnDefinition = "text")
    private String deputadoUri;

    @Column(length = 200)
    private String deputadoNome;

    @Column(length = 20)
    private String deputadoSiglaPartido;

    @Column(columnDefinition = "text")
    private String deputadoUriPartido;

    @Column(length = 4)
    private String deputadoSiglaUf;

    private Integer deputadoIdLegislatura;

    @Column(columnDefinition = "text")
    private String deputadoUrlFoto;
}
