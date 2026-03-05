package br.leg.congresso.etl.domain.enums;

public enum TipoExecucao {
    FULL,           // Carga completa histórica
    INCREMENTAL,    // Atualização dos últimos N dias
    FORCED,         // Reprocessamento forçado pelo usuário
    PROMOCAO,       // Promoção Silver → Gold
    ENRICHMENT      // Enriquecimento Silver com fontes secundárias
}
