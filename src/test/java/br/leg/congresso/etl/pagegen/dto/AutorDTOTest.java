package br.leg.congresso.etl.pagegen.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutorDTO — builder")
class AutorDTOTest {

    @Test
    @DisplayName("parlamentar da câmara com todos os campos")
    void parlamentarCamara_todosOsCampos() {
        var autor = AutorDTO.builder()
                .nome("Maria Souza")
                .tipo("Deputada Federal")
                .casa("camara")
                .idOriginal("204560")
                .proponente(true)
                .partido("PT")
                .uf("SP")
                .build();

        assertThat(autor.getNome()).isEqualTo("Maria Souza");
        assertThat(autor.getTipo()).isEqualTo("Deputada Federal");
        assertThat(autor.getCasa()).isEqualTo("camara");
        assertThat(autor.getIdOriginal()).isEqualTo("204560");
        assertThat(autor.isProponente()).isTrue();
        assertThat(autor.getPartido()).isEqualTo("PT");
        assertThat(autor.getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("entidade não parlamentar: casa e idOriginal nulos")
    void entidadeNaoParlamentar_casaNula() {
        var autor = AutorDTO.builder()
                .nome("Comissão de Finanças e Tributação")
                .tipo("Comissão")
                .proponente(false)
                .build();

        assertThat(autor.getCasa()).isNull();
        assertThat(autor.getIdOriginal()).isNull();
        assertThat(autor.isProponente()).isFalse();
    }

    @Test
    @DisplayName("senador com partido e UF")
    void senador_comPartidoUf() {
        var autor = AutorDTO.builder()
                .nome("Carlos Portinho")
                .tipo("Senador")
                .casa("senado")
                .idOriginal("5555")
                .partido("PL")
                .uf("RJ")
                .proponente(true)
                .build();

        assertThat(autor.getCasa()).isEqualTo("senado");
        assertThat(autor.getPartido()).isEqualTo("PL");
        assertThat(autor.getUf()).isEqualTo("RJ");
    }
}
