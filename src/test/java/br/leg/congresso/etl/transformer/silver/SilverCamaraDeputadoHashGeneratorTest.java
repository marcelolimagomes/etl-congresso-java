package br.leg.congresso.etl.transformer.silver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;

@DisplayName("SilverCamaraDeputadoHashGenerator — SHA-256 sobre campos CSV")
class SilverCamaraDeputadoHashGeneratorTest {

    private final SilverCamaraDeputadoHashGenerator generator = new SilverCamaraDeputadoHashGenerator();

    private SilverCamaraDeputado deputadoBase() {
        return SilverCamaraDeputado.builder()
                .camaraId("123")
                .nomeCivil("João da Silva")
                .nomeParlamentar("João")
                .nomeEleitoral("João Silva")
                .sexo("M")
                .dataNascimento("1970-05-15")
                .dataFalecimento(null)
                .ufNascimento("SP")
                .municipioNascimento("São Paulo")
                .cpf("12345678900")
                .escolaridade("Superior")
                .urlWebsite("https://exemplo.com")
                .urlFoto("https://foto.com/123.jpg")
                .primeiraLegislatura("50")
                .ultimaLegislatura("57")
                .build();
    }

    @Test
    @DisplayName("mesmos dados produzem o mesmo hash (determinismo)")
    void mesmosDadosProducemMesmoHash() {
        SilverCamaraDeputado d1 = deputadoBase();
        SilverCamaraDeputado d2 = deputadoBase();
        assertThat(generator.generate(d1)).isEqualTo(generator.generate(d2));
    }

    @Test
    @DisplayName("mudança em campo CSV produz hash diferente")
    void mudancaEmCampoProducHashDiferente() {
        SilverCamaraDeputado d1 = deputadoBase();
        SilverCamaraDeputado d2 = deputadoBase();
        d2.setNomeParlamentar("João Alterado");
        assertThat(generator.generate(d1)).isNotEqualTo(generator.generate(d2));
    }

    @Test
    @DisplayName("campos det_* não afetam o hash")
    void camposDetNaoAfetamHash() {
        SilverCamaraDeputado d1 = deputadoBase();
        SilverCamaraDeputado d2 = deputadoBase();
        d2.setDetStatusId("123");
        d2.setDetStatusSiglaPartido("PT");
        d2.setDetGabineteNome("Gabinete 5");
        assertThat(generator.generate(d1)).isEqualTo(generator.generate(d2));
    }

    @Test
    @DisplayName("hash tem 64 caracteres hexadecimais (SHA-256)")
    void hashTem64Caracteres() {
        String hash = generator.generate(deputadoBase());
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("campos nulos são tratados sem NullPointerException")
    void camposNulosNaoLancamExcecao() {
        SilverCamaraDeputado d = SilverCamaraDeputado.builder()
                .camaraId("999")
                .build();
        assertThat(generator.generate(d)).isNotNull().hasSize(64);
    }
}
