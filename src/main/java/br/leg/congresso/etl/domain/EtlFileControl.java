package br.leg.congresso.etl.domain;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controle de ingestão de arquivos CSV da Câmara.
 * Evita reprocessamento desnecessário via checksum SHA-256.
 */
@Entity
@Table(name = "etl_file_control", indexes = {
    @Index(name = "idx_file_control_status", columnList = "status, forcar_reprocessamento"),
    @Index(name = "idx_file_control_ano",    columnList = "ano_referencia")
}, uniqueConstraints = @UniqueConstraint(
    name = "uq_file_control",
    columnNames = {"origem", "nome_arquivo"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtlFileControl {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private CasaLegislativa origem = CasaLegislativa.CAMARA;

    @Column(name = "nome_arquivo", length = 255, nullable = false)
    private String nomeArquivo;

    @Column(name = "url_download", length = 500)
    private String urlDownload;

    /** SHA-256 do conteúdo — detecta mudanças sem reprocessar arquivos iguais */
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "ano_referencia")
    private Integer anoReferencia;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private StatusEtl status = StatusEtl.PENDING;

    @Column(name = "forcar_reprocessamento", nullable = false)
    @Builder.Default
    private boolean forcarReprocessamento = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private EtlJobControl job;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    public boolean deveProcessar(String novoChecksum) {
        if (forcarReprocessamento) return true;
        if (status == StatusEtl.PENDING) return true;
        if (checksumSha256 == null) return true;
        return !checksumSha256.equals(novoChecksum);
    }
}
