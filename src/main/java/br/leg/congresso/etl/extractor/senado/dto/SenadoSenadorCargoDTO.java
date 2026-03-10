package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para cargos de um senador.
 * GET /senador/{codigo}/cargos.json
 *
 * Envelope: CargoParlamentar → Parlamentar → Cargos → Cargo []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorCargoDTO {

    @JsonProperty("CargoParlamentar")
    private CargoParlamentar cargoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CargoParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Cargos")
        private Cargos cargos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cargos {
        @JsonProperty("Cargo")
        private List<Cargo> cargo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cargo {
        @JsonProperty("CodigoCargo")
        private String codigoCargo;

        @JsonProperty("DescricaoCargo")
        private String descricaoCargo;

        @JsonProperty("TipoCargo")
        private String tipoCargo;

        @JsonProperty("NomeOrgao")
        private String nomeOrgao;

        @JsonProperty("DataInicio")
        private String dataInicio;

        @JsonProperty("DataFim")
        private String dataFim;
    }
}
