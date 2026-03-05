package br.leg.congresso.etl.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentHashGenerator — geração de hashes SHA-256")
class ContentHashGeneratorTest {

    private ContentHashGenerator generator;

    @BeforeEach
    void setup() {
        generator = new ContentHashGenerator();
    }

    @Test
    @DisplayName("hash gerado é string hexadecimal de 64 caracteres (SHA-256)")
    void hashTemTamanhoCorreto() {
        String hash = generator.generateForProposicao(
                "CAMARA", "PL", 1234, 2024,
                "Ementa de teste", "Em tramitação", false);
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("mesmos dados sempre produzem o mesmo hash (determinístico)")
    void hashEDeterministico() {
        String h1 = generator.generateForProposicao("CAMARA","PL",1,2024,"ementa","sit",false);
        String h2 = generator.generateForProposicao("CAMARA","PL",1,2024,"ementa","sit",false);
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    @DisplayName("diferença em qualquer campo gera hash diferente")
    void camposDiferentesGeramHashDiferente() {
        String base = generator.generateForProposicao("CAMARA","PL",1,2024,"ementa","sit",false);

        assertThat(generator.generateForProposicao("SENADO","PL",1,2024,"ementa","sit",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PEC",1,2024,"ementa","sit",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PL",2,2024,"ementa","sit",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PL",1,2025,"ementa","sit",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PL",1,2024,"outra",  "sit",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PL",1,2024,"ementa","nova",false))
                .isNotEqualTo(base);
        assertThat(generator.generateForProposicao("CAMARA","PL",1,2024,"ementa","sit",true))
                .isNotEqualTo(base);
    }

    @Test
    @DisplayName("aceita valores nulos sem lançar exceção")
    void aceitaNulosSemErro() {
        assertThat(generator.generateForProposicao(null, null, null, null, null, null, null))
                .isNotNull().hasSize(64);
    }

    @Test
    @DisplayName("generateForString produz hash SHA-256 válido")
    void generateForStringFunciona() {
        String h = generator.generateForString("conteúdo de teste");
        assertThat(h).hasSize(64).matches("[a-f0-9]+");
        assertThat(generator.generateForString("conteúdo de teste")).isEqualTo(h);
    }

    @Test
    @DisplayName("generateForString(null) não lança NPE")
    void generateForStringNuloNaoLancaNPE() {
        assertThat(generator.generateForString(null)).isNotNull();
    }
}
