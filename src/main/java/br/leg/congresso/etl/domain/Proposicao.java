package br.leg.congresso.etl.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidade central do ETL.
 * Unifica proposições da Câmara e matérias do Senado.
 * Chave natural: (casa, sigla, numero, ano)
 */
@Entity
@Table(name = "proposicao", uniqueConstraints = @UniqueConstraint(name = "uq_proposicao_natural", columnNames = {
        "casa", "sigla", "numero", "ano" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "tramitacoes")
public class Proposicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CasaLegislativa casa;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private TipoProposicao tipo;

    @Column(length = 20)
    private String sigla;

    @Column
    private Integer numero;

    @Column
    private Integer ano;

    @Column(columnDefinition = "TEXT")
    private String ementa;

    @Column(length = 500)
    private String situacao;

    @Column(name = "despacho_atual", columnDefinition = "TEXT")
    private String despachoAtual;

    @Column(name = "data_apresentacao")
    private LocalDate dataApresentacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "status_final", length = 100)
    private String statusFinal;

    @Column(name = "virou_lei", nullable = false)
    private boolean virouLei = false;

    /** Identificador original na fonte (id Câmara ou código Senado) */
    @Column(name = "id_origem", length = 50)
    private String idOrigem;

    /** URL de referência na API de origem */
    @Column(name = "uri_origem", length = 500)
    private String uriOrigem;

    /** URL do inteiro teor do documento */
    @Column(name = "url_inteiro_teor", length = 1000)
    private String urlInteiroTeor;

    /** Palavras-chave associadas */
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    /**
     * Hash SHA-256 dos campos relevantes para detecção de mudanças.
     * Se o hash não mudar, o registro é ignorado no reprocessamento.
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "proposicao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tramitacao> tramitacoes = new ArrayList<>();

    /**
     * FK para silver.camara_proposicao — rastreabilidade bidirecional Silver→Gold
     */
    @Builder.Default
    @Column(name = "silver_camara_id")
    private UUID silverCamaraId = null;

    /** FK para silver.senado_materia — rastreabilidade bidirecional Silver→Gold */
    @Builder.Default
    @Column(name = "silver_senado_id")
    private UUID silverSenadoId = null;
}
