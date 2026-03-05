package br.leg.congresso.etl.transformer.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SilverCamaraHashGenerator — SHA-256 sobre campos fonte")
class SilverCamaraHashGeneratorTest {

    private SilverCamaraHashGenerator generator;

    @BeforeEach
    void setup() {
        generator = new SilverCamaraHashGenerator();
    }

    private SilverCamaraProposicao proposicaoBase() {
        return SilverCamaraProposicao.builder()
            .camaraId("12345")
            .siglaTipo("PL")
            .numero(100)
            .ano(2024)
            .ementa("Ementa de teste")
            .ementaDetalhada("")
            .dataApresentacao("2024-01-15")
            .ultimoStatusDataHora("2024-03-01T10:00:00")
            .ultimoStatusDescricaoSituacao("Em tramitação")
            .ultimoStatusDespacho("Encaminhado à comissão")
            .ultimoStatusSiglaOrgao("CCJC")
            .ultimoStatusRegime("Ordinário")
            .keywords("saúde;infraestrutura")
            .urlInteiroTeor("http://camara.leg.br/propostas/PL-100-2024")
            .build();
    }

    @Test
    @DisplayName("hash gerado é string hexadecimal de 64 caracteres (SHA-256)")
    void hashTemTamanhoCorreto() {
        String hash = generator.generate(proposicaoBase());
        assertThat(hash)
            .hasSize(64)
            .matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("mesmos dados sempre produzem o mesmo hash (determinístico)")
    void hashEDeterministico() {
        SilverCamaraProposicao p = proposicaoBase();
        assertThat(generator.generate(p)).isEqualTo(generator.generate(p));

        // Dois builders com mesmos valores também devem produzir hash igual
        SilverCamaraProposicao p2 = proposicaoBase();
        assertThat(generator.generate(p)).isEqualTo(generator.generate(p2));
    }

    @Test
    @DisplayName("diferença em camaraId gera hash diferente")
    void camaraIdDiferenteGeraHashDiferente() {
        SilverCamaraProposicao p1 = proposicaoBase();
        SilverCamaraProposicao p2 = SilverCamaraProposicao.builder()
            .camaraId("99999") // diferente
            .siglaTipo("PL").numero(100).ano(2024)
            .ementa("Ementa de teste").ementaDetalhada("")
            .dataApresentacao("2024-01-15")
            .ultimoStatusDataHora("2024-03-01T10:00:00")
            .ultimoStatusDescricaoSituacao("Em tramitação")
            .ultimoStatusDespacho("Encaminhado à comissão")
            .ultimoStatusSiglaOrgao("CCJC")
            .ultimoStatusRegime("Ordinário")
            .keywords("saúde;infraestrutura")
            .urlInteiroTeor("http://camara.leg.br/propostas/PL-100-2024")
            .build();

        assertThat(generator.generate(p1)).isNotEqualTo(generator.generate(p2));
    }

    @Test
    @DisplayName("diferença no ultimoStatusDespacho gera hash diferente")
    void despachoAlteradoMudaHash() {
        SilverCamaraProposicao original = proposicaoBase();
        SilverCamaraProposicao alterado = proposicaoBase();
        alterado.setUltimoStatusDespacho("Arquivado");

        assertThat(generator.generate(original)).isNotEqualTo(generator.generate(alterado));
    }

    @Test
    @DisplayName("campos nulos não lançam exceção — tratados como string vazia")
    void camposNulosNaoGeramExcecao() {
        SilverCamaraProposicao vazia = SilverCamaraProposicao.builder().build();
        assertThat(generator.generate(vazia))
            .isNotNull()
            .hasSize(64);
    }

    @Test
    @DisplayName("proposições com apenas ementa diferente geram hashes distintos")
    void ementaDiferenteGeraHashDistinto() {
        SilverCamaraProposicao p1 = proposicaoBase();
        SilverCamaraProposicao p2 = proposicaoBase();
        p2.setEmenta("Ementa completamente diferente");

        assertThat(generator.generate(p1)).isNotEqualTo(generator.generate(p2));
    }
}
