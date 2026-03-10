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
 * Entidade Silver — discursos de deputados da Câmara.
 * Fonte: API GET /deputados/{id}/discursos.
 * Chave de deduplicação: (camara_deputado_id, data_hora_inicio, tipo_discurso).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_discurso", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_dep_discurso_nat_key", columnNames = {
        "camara_deputado_id", "data_hora_inicio", "tipo_discurso" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoDiscurso {

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

    // ── Campos da API /deputados/{id}/discursos ────────────────────────────────
    @Column(name = "camara_deputado_id", length = 20)
    private String camaraDeputadoId;

    @Column(name = "data_hora_inicio", length = 30)
    private String dataHoraInicio;

    @Column(name = "data_hora_fim", length = 30)
    private String dataHoraFim;

    @Column(name = "tipo_discurso", length = 100)
    private String tipoDiscurso;

    @Column(name = "sumario", columnDefinition = "TEXT")
    private String sumario;

    @Column(name = "transcricao", columnDefinition = "TEXT")
    private String transcricao;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "url_texto", length = 500)
    private String urlTexto;

    @Column(name = "url_audio", length = 500)
    private String urlAudio;

    @Column(name = "url_video", length = 500)
    private String urlVideo;

    @Column(name = "uri_evento", length = 500)
    private String uriEvento;

    @Column(name = "fase_evento_titulo", length = 300)
    private String faseEventoTitulo;

    @Column(name = "fase_evento_data_hora_inicio", length = 30)
    private String faseEventoDataHoraInicio;

    @Column(name = "fase_evento_data_hora_fim", length = 30)
    private String faseEventoDataHoraFim;
}
