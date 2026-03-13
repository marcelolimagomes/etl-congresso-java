package br.leg.congresso.etl.extractor.camara.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("CamaraDeputadoDetalheDTO")
class CamaraDeputadoDetalheDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("desserializa descricaoStatus e gabinete.email do payload atual da API")
    void desserializaDescricaoStatusEGabineteEmail() throws Exception {
        String json = """
                {
                  "dados": {
                    "ultimoStatus": {
                      "email": null,
                      "gabinete": {
                        "email": "dep.teste@camara.leg.br"
                      },
                      "descricaoStatus": "Licenciado"
                    }
                  }
                }
                """;

        CamaraDeputadoDetalheDTO dto = objectMapper.readValue(json, CamaraDeputadoDetalheDTO.class);

        assertThat(dto.getDados()).isNotNull();
        assertThat(dto.getDados().getUltimoStatus()).isNotNull();
        assertThat(dto.getDados().getUltimoStatus().getDescricao()).isEqualTo("Licenciado");
        assertThat(dto.getDados().getUltimoStatus().getGabinete()).isNotNull();
        assertThat(dto.getDados().getUltimoStatus().getGabinete().getEmail())
                .isEqualTo("dep.teste@camara.leg.br");
    }
}