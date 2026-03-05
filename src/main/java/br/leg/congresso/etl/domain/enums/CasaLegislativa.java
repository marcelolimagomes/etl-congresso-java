package br.leg.congresso.etl.domain.enums;

public enum CasaLegislativa {
    CAMARA("Câmara dos Deputados"),
    SENADO("Senado Federal");

    private final String descricao;

    CasaLegislativa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
