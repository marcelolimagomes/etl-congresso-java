package br.leg.congresso.etl.transformer.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Gera hash SHA-256 sobre os campos brutos de SilverCamaraProposicao.
 * O hash é calculado sobre os campos-fonte (sem campos de controle Gold),
 * permitindo detectar mudanças no conteúdo original.
 */
@Component
public class SilverCamaraHashGenerator {

    public String generate(SilverCamaraProposicao p) {
        // Concatena os campos fonte mais relevantes para detecção de mudança
        String input = String.join("|",
            nvl(p.getCamaraId()),
            nvl(p.getSiglaTipo()),
            nvl(p.getNumero()),
            nvl(p.getAno()),
            nvl(p.getEmenta()),
            nvl(p.getEmentaDetalhada()),
            nvl(p.getDataApresentacao()),
            nvl(p.getUltimoStatusDataHora()),
            nvl(p.getUltimoStatusDescricaoSituacao()),
            nvl(p.getUltimoStatusDespacho()),
            nvl(p.getUltimoStatusSiglaOrgao()),
            nvl(p.getUltimoStatusRegime()),
            nvl(p.getKeywords()),
            nvl(p.getUrlInteiroTeor())
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
