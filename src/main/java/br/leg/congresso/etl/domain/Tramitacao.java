package br.leg.congresso.etl.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tramitacao", indexes = {
    @Index(name = "idx_tramitacao_proposicao", columnList = "proposicao_id"),
    @Index(name = "idx_tramitacao_data",       columnList = "data_evento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tramitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposicao_id", nullable = false)
    private Proposicao proposicao;

    @Column
    private Integer sequencia;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @Column(name = "sigla_orgao", length = 50)
    private String siglaOrgao;

    @Column(name = "descricao_orgao", length = 500)
    private String descricaoOrgao;

    @Column(name = "descricao_tramitacao", columnDefinition = "TEXT")
    private String descricaoTramitacao;

    @Column(name = "descricao_situacao", length = 500)
    private String descricaoSituacao;

    @Column(columnDefinition = "TEXT")
    private String despacho;

    @Column(length = 100)
    private String ambito;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    /** FK para silver.camara_tramitacao — rastreabilidade Silver→Gold */
    @Builder.Default
    @Column(name = "silver_camara_tramitacao_id")
    private UUID silverCamaraTramitacaoId = null;

    /** FK para silver.senado_movimentacao — rastreabilidade Silver→Gold */
    @Builder.Default
    @Column(name = "silver_senado_movimentacao_id")
    private UUID silverSenadoMovimentacaoId = null;
}
