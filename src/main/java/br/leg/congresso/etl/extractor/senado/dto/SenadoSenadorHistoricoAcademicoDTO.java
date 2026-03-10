package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para histórico acadêmico de um senador.
 * GET /senador/{codigo}/historicoAcademico.json
 *
 * Envelope: HistoricoAcademicoParlamentar → Parlamentar → HistoricoAcademico →
 * Curso []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorHistoricoAcademicoDTO {

    @JsonProperty("HistoricoAcademicoParlamentar")
    private HistoricoAcademicoParlamentar historicoAcademicoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoricoAcademicoParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("HistoricoAcademico")
        private HistoricoAcademico historicoAcademico;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoricoAcademico {
        @JsonProperty("Curso")
        private List<Curso> curso;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Curso {
        @JsonProperty("CodigoCurso")
        private String codigoCurso;

        @JsonProperty("NomeCurso")
        private String nomeCurso;

        @JsonProperty("Instituicao")
        private String instituicao;

        @JsonProperty("DescricaoInstituicao")
        private String descricaoInstituicao;

        @JsonProperty("GrauInstrucao")
        private String grauInstrucao;

        @JsonProperty("DataInicioCurso")
        private String dataInicioCurso;

        @JsonProperty("DataTerminoCurso")
        private String dataTerminoCurso;

        @JsonProperty("Concluido")
        private String concluido;
    }
}
