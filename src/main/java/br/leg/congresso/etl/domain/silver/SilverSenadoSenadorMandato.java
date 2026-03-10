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
 * Entidade Silver — mandatos por senador.
 * Fonte: API GET /senador/{codigo}/mandatos.
 * Chave de deduplicação: (codigo_senador, codigo_mandato).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_mandato", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_mandato", columnNames = {
        "codigo_senador", "codigo_mandato" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorMandato {

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

    @Column(name = "codigo_mandato", length = 20)
    private String codigoMandato;

    @Column(name = "descricao", length = 300)
    private String descricao;

    @Column(name = "uf_mandato", length = 5)
    private String ufMandato;

    @Column(name = "participacao", length = 20)
    private String participacao;

    @Column(name = "data_inicio", length = 30)
    private String dataInicio;

    @Column(name = "data_fim", length = 30)
    private String dataFim;

    @Column(name = "data_designacao", length = 30)
    private String dataDesignacao;

    @Column(name = "data_termino", length = 30)
    private String dataTermino;

    @Column(name = "entrou_exercicio", length = 20)
    private String entrouExercicio;

    @Column(name = "data_exercicio", length = 30)
    private String dataExercicio;
}
