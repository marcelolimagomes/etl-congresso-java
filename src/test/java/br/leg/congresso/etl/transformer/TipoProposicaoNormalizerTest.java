package br.leg.congresso.etl.transformer;

import br.leg.congresso.etl.domain.enums.TipoProposicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TipoProposicaoNormalizer — normalização de siglas")
class TipoProposicaoNormalizerTest {

    // ─── normalizar() ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "sigla \"{0}\" → {1}")
    @CsvSource({
        "PL,  LEI_ORDINARIA",
        "PLC, LEI_COMPLEMENTAR",
        "PLP, LEI_COMPLEMENTAR",
        "MPV, MEDIDA_PROVISORIA",
        "MP,  MEDIDA_PROVISORIA",
        "PEC, EMENDA_CONSTITUCIONAL",
        "PDL, DECRETO_LEGISLATIVO",
        "PDS, DECRETO_LEGISLATIVO",
        "PR,  RESOLUCAO",
        "PRN, RESOLUCAO"
    })
    void deveMappearSiglasConhecidas(String sigla, String esperado) {
        TipoProposicao resultado = TipoProposicaoNormalizer.normalizar(sigla.trim());
        assertThat(resultado).isEqualTo(TipoProposicao.valueOf(esperado.trim()));
    }

    @ParameterizedTest(name = "sigla em minúsculas \"{0}\" → normalizada")
    @ValueSource(strings = {"pl", "pec", "mpv", "pdl"})
    void deveSuportarMinusculas(String sigla) {
        TipoProposicao resultado = TipoProposicaoNormalizer.normalizar(sigla);
        assertThat(resultado).isNotEqualTo(TipoProposicao.OUTRO);
    }

    @Test
    @DisplayName("sigla desconhecida retorna OUTRO")
    void siglaDesconhecidaRetornaOutro() {
        assertThat(TipoProposicaoNormalizer.normalizar("XYZ")).isEqualTo(TipoProposicao.OUTRO);
        assertThat(TipoProposicaoNormalizer.normalizar("")).isEqualTo(TipoProposicao.OUTRO);
        assertThat(TipoProposicaoNormalizer.normalizar("   ")).isEqualTo(TipoProposicao.OUTRO);
    }

    @Test
    @DisplayName("sigla nula retorna OUTRO (sem NPE)")
    void siglaNulaRetornaOutro() {
        assertThat(TipoProposicaoNormalizer.normalizar(null)).isEqualTo(TipoProposicao.OUTRO);
    }

    // ─── isProposicaoAceita() ────────────────────────────────────────────────

    @ParameterizedTest(name = "sigla aceita: \"{0}\"")
    @ValueSource(strings = {"PL", "PLP", "MPV", "MP", "PEC", "PDL", "PR", "PLC", "PDS", "PRN"})
    void deveAceitarSiglasValidas(String sigla) {
        assertThat(TipoProposicaoNormalizer.isProposicaoAceita(sigla)).isTrue();
    }

    @ParameterizedTest(name = "sigla recusada: \"{0}\"")
    @ValueSource(strings = {"REQ", "MSC", "INC", "RIC", "OF", ""})
    void deveRejeitarSiglasInvalidas(String sigla) {
        assertThat(TipoProposicaoNormalizer.isProposicaoAceita(sigla)).isFalse();
    }

    @Test
    @DisplayName("isProposicaoAceita(null) retorna false sem NPE")
    void isAceitaNulaRetornaFalse() {
        assertThat(TipoProposicaoNormalizer.isProposicaoAceita(null)).isFalse();
    }

    @ParameterizedTest(name = "aceita minúsculas: \"{0}\"")
    @ValueSource(strings = {"pl", "pec", "mpv"})
    void aceitaMinusculasTambem(String sigla) {
        assertThat(TipoProposicaoNormalizer.isProposicaoAceita(sigla)).isTrue();
    }
}
