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
 * Entidade Silver — discursos por senador.
 * Fonte: API GET /senador/{codigo}/discursos.
 * Chave de deduplicação: (codigo_senador, codigo_discurso).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_discurso", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_discurso", columnNames = {
        "codigo_senador", "codigo_discurso" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorDiscurso {

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

    @Column(name = "codigo_senador", length = 20)
    private String codigoSenador;

    @Column(name = "codigo_discurso", length = 20)
    private String codigoDiscurso;

    @Column(name = "codigo_sessao", length = 20)
    private String codigoSessao;

    @Column(name = "data_pronunciamento", length = 30)
    private String dataPronunciamento;

    @Column(name = "casa", length = 10)
    private String casa;

    @Column(name = "tipo_sessao", length = 100)
    private String tipoSessao;

    @Column(name = "numero_sessao", length = 20)
    private String numeroSessao;

    @Column(name = "tipo_pronunciamento", length = 100)
    private String tipoPronunciamento;

    @Column(name = "texto_discurso", columnDefinition = "TEXT")
    private String textoDiscurso;

    @Column(name = "duracao_aparte", length = 20)
    private String duracaoAparte;

    @Column(name = "url_video", length = 500)
    private String urlVideo;

    @Column(name = "url_audio", length = 500)
    private String urlAudio;
}
