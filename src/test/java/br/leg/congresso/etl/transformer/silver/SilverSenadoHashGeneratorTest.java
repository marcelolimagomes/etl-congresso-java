package br.leg.congresso.etl.transformer.silver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;

@DisplayName("SilverSenadoHashGenerator — SHA-256 sobre campos fonte e metadados enriquecidos do Senado")
class SilverSenadoHashGeneratorTest {

    private SilverSenadoHashGenerator generator;

    @BeforeEach
    void setup() {
        generator = new SilverSenadoHashGenerator();
    }

    private SilverSenadoMateria materiaBase() {
        return SilverSenadoMateria.builder()
                .codigo("1234567")
                .sigla("PL")
                .numero("00042")
                .ano(2024)
                .ementa("Altera a Lei 9.394/1996 para fins educacionais")
                .autor("Sen. Fulano de Tal")
                .data("2024-03-15")
                .identificacaoProcesso("PL nº 42/2024")
                .detSiglaSubtipo("PL")
                .detIndicadorTramitando("Sim")
                .detIndexacao("educação; ensino fundamental")
                .detCasaIniciadora("SF")
                .detNaturezaNome("Ordinária")
                .detClassificacoes("[{\"codigoClasse\":\"1\"}]")
                .detOutrasInformacoes("[{\"nomeServico\":\"AutoriaMateria\"}]")
                .urlTexto("https://legis.senado.leg.br/textos/1234567")
                .build();
    }

    @Test
    @DisplayName("hash gerado é string hexadecimal de 64 caracteres (SHA-256)")
    void hashTemTamanhoCorreto() {
        String hash = generator.generate(materiaBase());
        assertThat(hash)
                .hasSize(64)
                .matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("mesmos dados produzem o mesmo hash (determinístico)")
    void hashEDeterministico() {
        SilverSenadoMateria m1 = materiaBase();
        SilverSenadoMateria m2 = materiaBase();
        assertThat(generator.generate(m1)).isEqualTo(generator.generate(m2));
    }

    @Test
    @DisplayName("diferença no código gera hash diferente")
    void codigoDiferenteGeraHashDiferente() {
        SilverSenadoMateria original = materiaBase();
        SilverSenadoMateria alterado = materiaBase();
        alterado.setCodigo("9999999");

        assertThat(generator.generate(original)).isNotEqualTo(generator.generate(alterado));
    }

    @Test
    @DisplayName("mudança na ementa gera hash diferente")
    void ementaAlteradaMudaHash() {
        SilverSenadoMateria original = materiaBase();
        SilverSenadoMateria alterado = materiaBase();
        alterado.setEmenta("Ementa totalmente diferente");

        assertThat(generator.generate(original)).isNotEqualTo(generator.generate(alterado));
    }

    @Test
    @DisplayName("mudança em detClassificacoes gera hash diferente")
    void detClassificacoesAlteradaMudaHash() {
        SilverSenadoMateria original = materiaBase();
        SilverSenadoMateria alterado = materiaBase();
        alterado.setDetClassificacoes("[{\"codigoClasse\":\"2\"}]");

        assertThat(generator.generate(original)).isNotEqualTo(generator.generate(alterado));
    }

    @Test
    @DisplayName("mudança em detOutrasInformacoes gera hash diferente")
    void detOutrasInformacoesAlteradaMudaHash() {
        SilverSenadoMateria original = materiaBase();
        SilverSenadoMateria alterado = materiaBase();
        alterado.setDetOutrasInformacoes("[{\"nomeServico\":\"TextoMateria\"}]");

        assertThat(generator.generate(original)).isNotEqualTo(generator.generate(alterado));
    }

    @Test
    @DisplayName("campos nulos não lançam exceção — tratados como string vazia")
    void camposNulosNaoGeramExcecao() {
        SilverSenadoMateria vazia = SilverSenadoMateria.builder().build();
        assertThat(generator.generate(vazia))
                .isNotNull()
                .hasSize(64);
    }
}
