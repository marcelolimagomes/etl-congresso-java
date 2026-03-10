package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para licenças de um senador.
 * GET /senador/{codigo}/licencas.json
 *
 * Envelope: LicencaParlamentar → Parlamentar → Licencas → Licenca []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorLicencaDTO {

    @JsonProperty("LicencaParlamentar")
    private LicencaParlamentar licencaParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LicencaParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Licencas")
        private Licencas licencas;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Licencas {
        @JsonProperty("Licenca")
        private List<Licenca> licenca;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Licenca {
        @JsonProperty("CodigoLicenca")
        private String codigoLicenca;

        @JsonProperty("DataInicio")
        private String dataInicio;

        @JsonProperty("DataFim")
        private String dataFim;

        @JsonProperty("SiglaMotivoLicenca")
        private String siglaMotivoLicenca;

        @JsonProperty("DescricaoMotivoLicenca")
        private String descricaoMotivoLicenca;
    }
}
