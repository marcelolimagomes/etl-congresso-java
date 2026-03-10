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
 * Entidade Silver — apartes por senador.
 * Fonte: API GET /senador/{codigo}/apartes.
 * Chave de deduplicação: (codigo_senador, codigo_aparte).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_aparte", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_aparte", columnNames = {
        "codigo_senador", "codigo_aparte" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorAparte {

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

    @Column(name = "codigo_aparte", length = 20)
    private String codigoAparte;

    @Column(name = "codigo_discurso_principal", length = 20)
    private String codigoDiscursoPrincipal;

    @Column(name = "codigo_sessao", length = 20)
    private String codigoSessao;

    @Column(name = "data_pronunciamento", length = 30)
    private String dataPronunciamento;

    @Column(name = "casa", length = 10)
    private String casa;

    @Column(name = "texto_aparte", columnDefinition = "TEXT")
    private String textoAparte;

    @Column(name = "url_video", length = 500)
    private String urlVideo;
}
