package br.leg.congresso.etl.admin.dto;

/**
 * DTO de status da camada Silver.
 * Retornado por GET /admin/etl/silver/status.
 */
public record SilverStatusDTO(
    Integer anoFiltro,
    long camaraProposicoesTotal,
    long camaraTramitacoesTotal,
    long senadoMateriasTotal,
    long senadoMateriasPendentesEnriquecimento,
    long senadoMovimentacoesTotal,
    long goldCamaraProposicoesTotal,
    long goldSenadoProposicoesTotal
) {
    /**
     * Indica se há registros do Senado ainda sem enriquecimento de detalhe.
     */
    public boolean isSenadoPendenteEnriquecimento() {
        return senadoMateriasPendentesEnriquecimento > 0;
    }


}
