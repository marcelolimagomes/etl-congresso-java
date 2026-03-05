package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoTextoMateriaDTO {

    @JsonProperty("TextoMateria")
    private TextoMateria textoMateria;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextoMateria {
        @JsonProperty("Materia")
        private Materia materia;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Materia {
        @JsonProperty("Textos")
        private Textos textos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Textos {
        @JsonProperty("Texto")
        private List<Texto> texto;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Texto {
        @JsonProperty("UrlTexto")
        private String urlTexto;

        @JsonProperty("DataTexto")
        private String dataTexto;
    }
}
