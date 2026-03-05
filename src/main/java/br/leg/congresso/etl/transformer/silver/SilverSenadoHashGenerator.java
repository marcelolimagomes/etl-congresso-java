package br.leg.congresso.etl.transformer.silver;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Gera hash SHA-256 sobre os campos brutos de SilverSenadoMateria.
 * Permite detectar mudanças no conteúdo original da API do Senado sem writes desnecessários.
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
            nvl(m.getUrlTexto())
        );

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
