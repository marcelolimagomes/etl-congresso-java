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
 * Entidade Silver — presenças de deputados em eventos da Câmara.
 * Espelha o CSV eventosPresencaDeputados-{ano}.csv.
 * Chave de deduplicação: (id_deputado, id_evento).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_presenca_evento", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_presenca_evento_nat_key", columnNames = {
        "id_deputado", "id_evento" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoPresencaEvento {

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

    // ── Campos do CSV eventosPresencaDeputados-{ano}.csv ───────────────────────
    @Column(name = "id_deputado", length = 20)
    private String idDeputado;

    @Column(name = "id_evento", length = 20)
    private String idEvento;

    @Column(name = "data_hora_inicio", length = 30)
    private String dataHoraInicio;

    @Column(name = "data_hora_fim", length = 30)
    private String dataHoraFim;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "descricao_tipo", length = 100)
    private String descricaoTipo;

    @Column(name = "situacao", length = 50)
    private String situacao;

    @Column(name = "uri_evento", length = 500)
    private String uriEvento;
}
