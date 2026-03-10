package br.leg.congresso.etl.pagegen.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProposicaoPageDTO — builder e defaults")
class ProposicaoPageDTOTest {

    @Test
    @DisplayName("builder com campos obrigatórios resulta em listas não-nulas por padrão")
    void listasPadrao_naoNulas() {
        var dto = ProposicaoPageDTO.builder()
                .casa("camara")
                .casaLabel("Câmara dos Deputados")
                .idOriginal("123")
                .siglaTipo("PL")
                .descricaoTipo("Projeto de Lei")
                .numero("1")
                .ano(2024)
                .ementa("Ementa.")
                .build();

        assertThat(dto.getKeywords()).isNotNull().isEmpty();
        assertThat(dto.getTemas()).isNotNull().isEmpty();
        assertThat(dto.getAutores()).isNotNull().isEmpty();
        assertThat(dto.getTramitacoes()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("builder permite sobrescrever listas")
    void listasSobrescritasComValores() {
        var dto = ProposicaoPageDTO.builder()
                .casa("senado")
                .casaLabel("Senado Federal")
                .idOriginal("999")
                .siglaTipo("PEC")
                .descricaoTipo("Proposta de Emenda à Constituição")
                .numero("10")
                .ano(2023)
                .ementa("Ementa PEC.")
                .keywords(List.of("saúde", "educação"))
                .temas(List.of("Saúde"))
                .build();

        assertThat(dto.getKeywords()).containsExactly("saúde", "educação");
        assertThat(dto.getTemas()).containsExactly("Saúde");
    }

    @Test
    @DisplayName("campos de identificação são imutáveis")
    void camposIdentificacaoImutaveis() {
        var dto = ProposicaoPageDTO.builder()
                .casa("camara")
                .casaLabel("Câmara dos Deputados")
                .idOriginal("2342835")
                .siglaTipo("PL")
                .descricaoTipo("Projeto de Lei")
                .numero("1234")
                .ano(2024)
                .ementa("Ementa.")
                .build();

        assertThat(dto.getCasa()).isEqualTo("camara");
        assertThat(dto.getCasaLabel()).isEqualTo("Câmara dos Deputados");
        assertThat(dto.getSiglaTipo()).isEqualTo("PL");
        assertThat(dto.getNumero()).isEqualTo("1234");
        assertThat(dto.getAno()).isEqualTo(2024);
        assertThat(dto.getEmenta()).isEqualTo("Ementa.");
    }
}
