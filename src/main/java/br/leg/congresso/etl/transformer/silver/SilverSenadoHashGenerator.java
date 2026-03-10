package br.leg.congresso.etl.transformer.silver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;

/**
 * Gera hash SHA-256 sobre os campos brutos de SilverSenadoMateria.
 * Inclui também os campos JSONB de detalhe para detectar mudanças de metadados
 * enriquecidos sem writes desnecessários.
 */
@Component
public class SilverSenadoHashGenerator {

    public String generate(SilverSenadoMateria m) {
        String input = String.join("|",
                nvl(m.getCodigo()),
                nvl(m.getSigla()),
                nvl(m.getNumero()),
                nvl(m.getAno()),
                nvl(m.getEmenta()),
                nvl(m.getAutor()),
                nvl(m.getData()),
                nvl(m.getIdentificacaoProcesso()),
                nvl(m.getDetSiglaSubtipo()),
                nvl(m.getDetIndicadorTramitando()),
                nvl(m.getDetIndexacao()),
                nvl(m.getDetCasaIniciadora()),
                nvl(m.getDetNaturezaNome()),
                nvl(m.getDetClassificacoes()),
                nvl(m.getDetOutrasInformacoes()),
                nvl(m.getUrlTexto()));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private String nvl(Object value) {
        return value != null ? value.toString() : "";
    }
}
