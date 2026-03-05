package br.leg.congresso.etl.domain.enums;

/**
 * Tipos de proposição que podem virar lei, mapeados das siglas originais
 * para tipos normalizados conforme spec-etp-materias-proposicoes.md
 */
public enum TipoProposicao {

    LEI_ORDINARIA("PL", "Projeto de Lei"),
    LEI_COMPLEMENTAR("PLP", "Projeto de Lei Complementar"),
    MEDIDA_PROVISORIA("MPV", "Medida Provisória"),
    EMENDA_CONSTITUCIONAL("PEC", "Proposta de Emenda à Constituição"),
    DECRETO_LEGISLATIVO("PDL", "Projeto de Decreto Legislativo"),
    RESOLUCAO("PR", "Projeto de Resolução"),
    OUTRO("OUTRO", "Outro tipo de proposição");

    private final String sigla;
    private final String descricao;

    TipoProposicao(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoProposicao fromSigla(String sigla) {
        if (sigla == null) return OUTRO;
        for (TipoProposicao tipo : values()) {
            if (tipo.sigla.equalsIgnoreCase(sigla.trim())) {
                return tipo;
            }
        }
        // Aliases adicionais do Senado
        return switch (sigla.trim().toUpperCase()) {
            case "MP"  -> MEDIDA_PROVISORIA;
            case "PLC" -> LEI_COMPLEMENTAR;
            case "PRN" -> RESOLUCAO;
            case "PDS" -> DECRETO_LEGISLATIVO;
            default    -> OUTRO;
        };
    }
}
