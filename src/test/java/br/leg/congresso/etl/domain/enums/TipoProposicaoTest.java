package br.leg.congresso.etl.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TipoProposicao — mapeamento de siglas via fromSigla()")
class TipoProposicaoTest {

    @ParameterizedTest(name = "fromSigla(\"{0}\") → {1}")
    @CsvSource({
        "PL,  LEI_ORDINARIA",
        "PLP, LEI_COMPLEMENTAR",
        "PLC, LEI_COMPLEMENTAR",
        "MPV, MEDIDA_PROVISORIA",
        "MP,  MEDIDA_PROVISORIA",
        "PEC, EMENDA_CONSTITUCIONAL",
        "PDL, DECRETO_LEGISLATIVO",
        "PDS, DECRETO_LEGISLATIVO",
        "PR,  RESOLUCAO",
        "PRN, RESOLUCAO"
    })
    void mapeaSiglasConhecidas(String sigla, String esperado) {
        TipoProposicao tipo = TipoProposicao.fromSigla(sigla.trim());
        assertThat(tipo).isEqualTo(TipoProposicao.valueOf(esperado.trim()));
    }

    @Test
    @DisplayName("sigla desconhecida → OUTRO")
    void siglaDesconhecidaRetornaOutro() {
        assertThat(TipoProposicao.fromSigla("XYZ")).isEqualTo(TipoProposicao.OUTRO);
    }

    @Test
    @DisplayName("null → OUTRO")
    void siglaNulaRetornaOutro() {
        assertThat(TipoProposicao.fromSigla(null)).isEqualTo(TipoProposicao.OUTRO);
    }

    @Test
    @DisplayName("fromSigla() é case-insensitive")
    void caseInsensitive() {
        assertThat(TipoProposicao.fromSigla("pl")).isEqualTo(TipoProposicao.LEI_ORDINARIA);
        assertThat(TipoProposicao.fromSigla("pec")).isEqualTo(TipoProposicao.EMENDA_CONSTITUCIONAL);
        assertThat(TipoProposicao.fromSigla("MPV")).isEqualTo(TipoProposicao.MEDIDA_PROVISORIA);
    }

    @Test
    @DisplayName("todos os valores do enum possuem sigla e descrição")
    void todosValoresPossuemSiglaEDescricao() {
        for (TipoProposicao tipo : TipoProposicao.values()) {
            assertThat(tipo.getSigla()).isNotBlank();
            assertThat(tipo.getDescricao()).isNotBlank();
        }
    }
}
