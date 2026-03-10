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
 * Entidade Silver — eventos de deputados da Câmara.
 * Fonte: API GET /deputados/{id}/eventos.
 * Chave de deduplicação: (camara_deputado_id, id_evento).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_evento", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_dep_evento_nat_key", columnNames = {
        "camara_deputado_id", "id_evento" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoEvento {

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
    private String origemCarga = "API";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos da API /deputados/{id}/eventos ──────────────────────────────────
    @Column(name = "camara_deputado_id", length = 20)
    private String camaraDeputadoId;

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

    @Column(name = "local_externo", length = 300)
    private String localExterno;

    @Column(name = "uri", length = 500)
    private String uri;

    @Column(name = "url_registro", length = 500)
    private String urlRegistro;

    @Column(name = "local_camara_nome", length = 200)
    private String localCamaraNome;

    @Column(name = "local_camara_predio", length = 100)
    private String localCamaraPredio;

    @Column(name = "local_camara_sala", length = 50)
    private String localCamaraSala;

    @Column(name = "local_camara_andar", length = 20)
    private String localCamaraAndar;

    @Column(name = "orgaos", columnDefinition = "TEXT")
    private String orgaos;
}
