package br.leg.congresso.etl.extractor.senado.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("SenadoRelatoriaDTO — desserialização JSON")
class SenadoRelatoriaDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("deve desserializar objeto de relatoria completo")
    void desserializar_relatoriaCompleta() throws Exception {
        String json = """
                {
                  "id": 9878123,
                  "casaRelator": "SF",
                  "idTipoRelator": 1,
                  "descricaoTipoRelator": "Relator",
                  "dataDesignacao": "2024-12-17",
                  "dataDestituicao": "2024-12-18",
                  "descricaoTipoEncerramento": "Deliberação da matéria",
                  "idProcesso": 8774796,
                  "codigoMateria": 166705,
                  "identificacaoProcesso": "PDL 361/2024",
                  "tramitando": "N",
                  "codigoParlamentar": 6009,
                  "nomeParlamentar": "Astronauta Marcos Pontes",
                  "nomeCompleto": "Marcos Cesar Pontes",
                  "sexoParlamentar": "M",
                  "formaTratamentoParlamentar": "Senador",
                  "siglaPartidoParlamentar": "PL",
                  "ufParlamentar": "SP",
                  "codigoColegiado": 1998,
                  "siglaCasa": "SF",
                  "siglaColegiado": "PLEN",
                  "nomeColegiado": "Plenário do Senado Federal",
                  "codigoTipoColegiado": 128
                }
                """;

        SenadoRelatoriaDTO dto = mapper.readValue(json, SenadoRelatoriaDTO.class);

        assertThat(dto.getId()).isEqualTo(9878123L);
        assertThat(dto.getCasaRelator()).isEqualTo("SF");
        assertThat(dto.getIdTipoRelator()).isEqualTo(1L);
        assertThat(dto.getDescricaoTipoRelator()).isEqualTo("Relator");
        assertThat(dto.getDataDesignacao()).isEqualTo("2024-12-17");
        assertThat(dto.getDataDestituicao()).isEqualTo("2024-12-18");
        assertThat(dto.getDescricaoTipoEncerramento()).isEqualTo("Deliberação da matéria");
        assertThat(dto.getIdProcesso()).isEqualTo(8774796L);
        assertThat(dto.getCodigoMateria()).isEqualTo(166705L);
        assertThat(dto.getIdentificacaoProcesso()).isEqualTo("PDL 361/2024");
        assertThat(dto.getTramitando()).isEqualTo("N");
        assertThat(dto.getCodigoParlamentar()).isEqualTo(6009L);
        assertThat(dto.getNomeParlamentar()).isEqualTo("Astronauta Marcos Pontes");
        assertThat(dto.getNomeCompleto()).isEqualTo("Marcos Cesar Pontes");
        assertThat(dto.getSexoParlamentar()).isEqualTo("M");
        assertThat(dto.getFormaTratamentoParlamentar()).isEqualTo("Senador");
        assertThat(dto.getSiglaPartidoParlamentar()).isEqualTo("PL");
        assertThat(dto.getUfParlamentar()).isEqualTo("SP");
        assertThat(dto.getCodigoColegiado()).isEqualTo(1998L);
        assertThat(dto.getSiglaCasa()).isEqualTo("SF");
        assertThat(dto.getSiglaColegiado()).isEqualTo("PLEN");
        assertThat(dto.getNomeColegiado()).isEqualTo("Plenário do Senado Federal");
        assertThat(dto.getCodigoTipoColegiado()).isEqualTo(128L);
    }

    @Test
    @DisplayName("deve desserializar relatoria com campos opcionais ausentes")
    void desserializar_relatoriaMinima() throws Exception {
        String json = """
                {
                  "id": 1234567,
                  "casaRelator": "SF",
                  "dataDesignacao": "2024-01-15"
                }
                """;

        SenadoRelatoriaDTO dto = mapper.readValue(json, SenadoRelatoriaDTO.class);

        assertThat(dto.getId()).isEqualTo(1234567L);
        assertThat(dto.getCasaRelator()).isEqualTo("SF");
        assertThat(dto.getDataDesignacao()).isEqualTo("2024-01-15");
        assertThat(dto.getNomeParlamentar()).isNull();
        assertThat(dto.getSiglaPartidoParlamentar()).isNull();
        assertThat(dto.getNomeColegiado()).isNull();
    }

    @Test
    @DisplayName("deve ignorar campos desconhecidos")
    void desserializar_ignoraCamposExtras() throws Exception {
        String json = """
                {
                  "id": 999,
                  "casaRelator": "SF",
                  "urlFotoParlamentar": "http://example.com/foto.jpg",
                  "urlPaginaParlamentar": "http://example.com/pagina",
                  "emailParlamentar": "sen@senado.leg.br",
                  "campoFuturoDesconhecido": "valor"
                }
                """;

        SenadoRelatoriaDTO dto = mapper.readValue(json, SenadoRelatoriaDTO.class);

        assertThat(dto.getId()).isEqualTo(999L);
        assertThat(dto.getCasaRelator()).isEqualTo("SF");
    }
}
