package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para o endpoint de autoria de matéria do Senado Federal.
 * GET /dadosabertos/materia/autoria/{codMateria}.json
 *
 * Endpoint legado (deprecated). Preserva fidelidade ao payload:
 * campos extras são ignorados via @JsonIgnoreProperties.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoAutoriaDTO {

    @JsonAlias({ "AutoriaMateria", "AutoriaBasicaMateria" })
    @JsonProperty("AutoriaMateria")
    private AutoriaMateria autoriaMateria;

    /** Compatibilidade com código que ainda referencia o nome antigo. */
    public AutoriaMateria getAutoriaBasicaMateria() {
        return autoriaMateria;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AutoriaMateria {

        @JsonProperty("Materia")
        private MateriaAutoria materia;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MateriaAutoria {

        @JsonProperty("CodigoMateria")
        private String codigoMateria;

        @JsonAlias({ "Autores", "Autoria" })
        @JsonProperty("Autoria")
        private AutoresWrapper autores;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AutoresWrapper {

        @JsonProperty("Autor")
        private List<Autor> autor;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Autor {

        @JsonProperty("NomeAutor")
        private String nomeAutor;

        @JsonProperty("SexoAutor")
        private String sexoAutor;

        /**
         * Campo plano presente na versão atual da API (substitui o antigo objeto
         * TipoAutor).
         */
        @JsonProperty("SiglaTipoAutor")
        private String siglaTipoAutor;

        @JsonProperty("DescricaoTipoAutor")
        private String descricaoTipoAutor;

        /** Mantido por compatibilidade; null na versão atual da API. */
        @JsonProperty("TipoAutor")
        private TipoAutor tipoAutor;

        @JsonProperty("PartidodoParlamentar")
        private PartidoParlamentar partidodoParlamentar;

        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TipoAutor {

        @JsonProperty("CodigoTipoAutor")
        private String codigoTipoAutor;

        @JsonProperty("DescricaoTipoAutor")
        private String descricaoTipoAutor;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartidoParlamentar {

        @JsonProperty("SiglaPartidoParlamentar")
        private String siglaPartidoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {

        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;

        @JsonProperty("NomeParlamentar")
        private String nomeParlamentar;

        @JsonProperty("SiglaPartidoParlamentar")
        private String siglaPartidoParlamentar;

        @JsonProperty("UfParlamentar")
        private String ufParlamentar;
    }
}
