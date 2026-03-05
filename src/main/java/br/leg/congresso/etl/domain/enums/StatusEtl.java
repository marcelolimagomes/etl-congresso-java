package br.leg.congresso.etl.domain.enums;

public enum StatusEtl {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    PARTIAL,   // Concluído com alguns erros
    SKIPPED    // Ignorado por controle de idempotência
}
