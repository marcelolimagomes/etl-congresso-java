package br.leg.congresso.etl.transformer.silver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;

/**
 * Gera hash SHA-256 sobre os campos brutos de SilverCamaraDeputado (campos do
 * CSV).
 * O hash é calculado sobre os campos-fonte (sem campos de controle nem det_*),
 * permitindo detectar mudanças no conteúdo original.
 */
@Component
public class SilverCamaraDeputadoHashGenerator {

    public String generate(SilverCamaraDeputado d) {
        String input = String.join("|",
                nvl(d.getCamaraId()),
                nvl(d.getNomeCivil()),
                nvl(d.getNomeParlamentar()),
                nvl(d.getNomeEleitoral()),
                nvl(d.getSexo()),
                nvl(d.getDataNascimento()),
                nvl(d.getDataFalecimento()),
                nvl(d.getUfNascimento()),
                nvl(d.getMunicipioNascimento()),
                nvl(d.getCpf()),
                nvl(d.getEscolaridade()),
                nvl(d.getUrlWebsite()),
                nvl(d.getUrlFoto()),
                nvl(d.getPrimeiraLegislatura()),
                nvl(d.getUltimaLegislatura()));

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
