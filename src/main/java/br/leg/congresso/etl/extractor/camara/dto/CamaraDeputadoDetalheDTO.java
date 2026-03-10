package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para a resposta do endpoint GET /api/v2/deputados/{id}.
 * Mapeia {@code dados} e {@code dados.ultimoStatus} para popular os campos
 * {@code det_*} em {@code silver.camara_deputado}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraDeputadoDetalheDTO {

    private Dados dados;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dados {
        private Integer id;
        private String uri;
        private String nome;
        private String nomeCivil;
        private String cpf;
        private String sexo;
        private String urlWebsite;
        private List<String> redeSocial;
        private String dataNascimento;
        private String dataFalecimento;
        private String ufNascimento;
        private String municipioNascimento;
        private String escolaridade;
        private UltimoStatus ultimoStatus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UltimoStatus {
        private Integer id;
        private String nome;
        private String siglaPartido;
        private String uriPartido;
        private String siglaUf;
        private Integer idLegislatura;
        private String urlFoto;
        private String email;
        private String data;
        private String nomeEleitoral;
        private Gabinete gabinete;
        private String situacao;
        private String condicaoEleitoral;
        private String descricao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Gabinete {
        private String nome;
        private String predio;
        private String sala;
        private String andar;
        private String telefone;
        private String email;
    }
}
