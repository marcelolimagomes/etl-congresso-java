package br.leg.congresso.etl.extractor.senado.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testes de desserialização Jackson para SenadoDetalheDTO.
 */
@DisplayName("SenadoDetalheDTO — desserialização Jackson")
class SenadoDetalheDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("@JsonProperty SiglaCasaIniciadora — campo principal desserializa corretamente")
    void siglaCasaIniciadora_desserializa_via_jsonProperty() throws Exception {
        String json = """
                {
                  "SiglaCasaIniciadora": "SF"
                }
                """;

        SenadoDetalheDTO.DadosBasicos dados = mapper.readValue(json, SenadoDetalheDTO.DadosBasicos.class);

        assertThat(dados.getSiglaCasaIniciadora()).isEqualTo("SF");
    }

    @Test
    @DisplayName("@JsonAlias CasaIniciadoraNoLegislativo — campo alternativo desserializa para siglaCasaIniciadora (fix 6.3)")
    void siglaCasaIniciadora_desserializa_via_jsonAlias() throws Exception {
        String json = """
                {
                  "CasaIniciadoraNoLegislativo": "CD"
                }
                """;

        SenadoDetalheDTO.DadosBasicos dados = mapper.readValue(json, SenadoDetalheDTO.DadosBasicos.class);

        assertThat(dados.getSiglaCasaIniciadora()).isEqualTo("CD");
    }

    @Test
    @DisplayName("quando ambos campos existem com o mesmo valor, mapeamento permanece consistente")
    void siglaCasaIniciadora_ambosCamposMesmoValor_mapeiaCorretamente() throws Exception {
        String json = """
                {
                  "SiglaCasaIniciadora": "SF",
                  "CasaIniciadoraNoLegislativo": "SF"
                }
                """;

        SenadoDetalheDTO.DadosBasicos dados = mapper.readValue(json, SenadoDetalheDTO.DadosBasicos.class);

        assertThat(dados.getSiglaCasaIniciadora()).isEqualTo("SF");
    }

    @Test
    @DisplayName("estrutura completa — IdentificacaoMateria e DadosBasicos desserializam corretamente")
    void detalheMateria_estrutura_completa_desserializa() throws Exception {
        String json = """
                {
                  "DetalheMateria": {
                    "Materia": {
                      "IdentificacaoMateria": {
                        "SiglaCasaIdentificacaoMateria": "SF",
                        "SiglaSubtipoMateria": "PL",
                        "IndicadorTramitando": "Sim"
                      },
                      "DadosBasicosMateria": {
                        "CasaIniciadoraNoLegislativo": "SF"
                      }
                    }
                  }
                }
                """;

        SenadoDetalheDTO dto = mapper.readValue(json, SenadoDetalheDTO.class);

        // Campos de identificação estão em IdentificacaoMateria
        var identificacao = dto.getDetalheMateria().getMateria().getIdentificacaoMateria();
        assertThat(identificacao.getSiglaCasaIdentificacao()).isEqualTo("SF");
        assertThat(identificacao.getSiglaSubtipo()).isEqualTo("PL");
        assertThat(identificacao.getIndicadorTramitando()).isEqualTo("Sim");

        // Fix 6.3: CasaIniciadoraNoLegislativo mapeado via @JsonAlias em DadosBasicos
        var dadosBasicos = dto.getDetalheMateria().getMateria().getDadosBasicosMateria();
        assertThat(dadosBasicos.getSiglaCasaIniciadora()).isEqualTo("SF");
    }
}
