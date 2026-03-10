package br.leg.congresso.etl.extractor.senado.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testa a desserialização do SenadoAutoriaDTO a partir de JSON.
 */
@DisplayName("SenadoAutoriaDTO — desserialização Jackson")
class SenadoAutoriaDTOTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String JSON_COM_PARLAMENTAR = """
            {
              "AutoriaBasicaMateria": {
                "Materia": {
                  "CodigoMateria": "131506",
                  "Autores": {
                    "Autor": [
                      {
                        "NomeAutor": "João da Silva",
                        "SexoAutor": "M",
                        "TipoAutor": {
                          "CodigoTipoAutor": "1",
                          "DescricaoTipoAutor": "Senador"
                        },
                        "IdentificacaoParlamentar": {
                          "CodigoParlamentar": "5678",
                          "NomeParlamentar": "João da Silva",
                          "SiglaPartidoParlamentar": "PT",
                          "UfParlamentar": "SP"
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

    private static final String JSON_COM_ENTIDADE = """
            {
              "AutoriaBasicaMateria": {
                "Materia": {
                  "CodigoMateria": "162431",
                  "Autores": {
                    "Autor": [
                      {
                        "NomeAutor": "Câmara dos Deputados",
                        "TipoAutor": {
                          "CodigoTipoAutor": "99",
                          "DescricaoTipoAutor": "Órgão Executivo"
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

    private static final String JSON_CAMPOS_EXTRAS = """
            {
              "AutoriaBasicaMateria": {
                "Materia": {
                  "CodigoMateria": "100",
                  "CampoDesconhecido": "ignorar",
                  "Autores": {
                    "Autor": [
                      {
                        "NomeAutor": "Senado Federal",
                        "CampoExtra": "deve ser ignorado",
                        "TipoAutor": {
                          "CodigoTipoAutor": "2",
                          "DescricaoTipoAutor": "Senado"
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

    @Test
    @DisplayName("deve desserializar autoria com parlamentar identificado")
    void desserializar_autorComParlamentar() throws Exception {
        SenadoAutoriaDTO dto = MAPPER.readValue(JSON_COM_PARLAMENTAR, SenadoAutoriaDTO.class);

        assertThat(dto.getAutoriaBasicaMateria()).isNotNull();
        assertThat(dto.getAutoriaBasicaMateria().getMateria()).isNotNull();
        assertThat(dto.getAutoriaBasicaMateria().getMateria().getCodigoMateria()).isEqualTo("131506");

        List<SenadoAutoriaDTO.Autor> autores = dto.getAutoriaBasicaMateria().getMateria().getAutores().getAutor();
        assertThat(autores).hasSize(1);

        SenadoAutoriaDTO.Autor autor = autores.get(0);
        assertThat(autor.getNomeAutor()).isEqualTo("João da Silva");
        assertThat(autor.getSexoAutor()).isEqualTo("M");
        assertThat(autor.getTipoAutor().getCodigoTipoAutor()).isEqualTo("1");
        assertThat(autor.getTipoAutor().getDescricaoTipoAutor()).isEqualTo("Senador");

        SenadoAutoriaDTO.IdentificacaoParlamentar parl = autor.getIdentificacaoParlamentar();
        assertThat(parl).isNotNull();
        assertThat(parl.getCodigoParlamentar()).isEqualTo("5678");
        assertThat(parl.getNomeParlamentar()).isEqualTo("João da Silva");
        assertThat(parl.getSiglaPartidoParlamentar()).isEqualTo("PT");
        assertThat(parl.getUfParlamentar()).isEqualTo("SP");
    }

    @Test
    @DisplayName("deve desserializar autoria com entidade (sem parlamentar identificado)")
    void desserializar_autorEntidade_semParlamentar() throws Exception {
        SenadoAutoriaDTO dto = MAPPER.readValue(JSON_COM_ENTIDADE, SenadoAutoriaDTO.class);

        List<SenadoAutoriaDTO.Autor> autores = dto.getAutoriaBasicaMateria().getMateria().getAutores().getAutor();
        assertThat(autores).hasSize(1);

        SenadoAutoriaDTO.Autor autor = autores.get(0);
        assertThat(autor.getNomeAutor()).isEqualTo("Câmara dos Deputados");
        assertThat(autor.getSexoAutor()).isNull();
        assertThat(autor.getIdentificacaoParlamentar()).isNull();
        assertThat(autor.getTipoAutor().getCodigoTipoAutor()).isEqualTo("99");
    }

    @Test
    @DisplayName("deve ignorar campos desconhecidos sem lançar exceção")
    void desserializar_ignoraCamposExtras() throws Exception {
        SenadoAutoriaDTO dto = MAPPER.readValue(JSON_CAMPOS_EXTRAS, SenadoAutoriaDTO.class);

        List<SenadoAutoriaDTO.Autor> autores = dto.getAutoriaBasicaMateria().getMateria().getAutores().getAutor();
        assertThat(autores).hasSize(1);
        assertThat(autores.get(0).getNomeAutor()).isEqualTo("Senado Federal");
    }
}
