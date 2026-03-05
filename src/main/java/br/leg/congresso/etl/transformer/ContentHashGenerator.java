package br.leg.congresso.etl.transformer;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Gera hash SHA-256 dos campos relevantes de uma proposição.
 * Utilizado para detecção de mudanças sem reprocessar registros idênticos.
 */
@Component
public class ContentHashGenerator {

    public String generateForProposicao(
            String casa,
            String sigla,
            Integer numero,
            Integer ano,
            String ementa,
            String situacao,
            Boolean virouLei) {

        return generateForProposicao(
            casa,
            sigla,
            numero,
            ano,
            ementa,
            situacao,
            virouLei,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public String generateForProposicao(
            String casa,
            String sigla,
            Integer numero,
            Integer ano,
            String ementa,
            String situacao,
            Boolean virouLei,
            String idOrigem,
            String uriOrigem,
            String despachoAtual,
            String statusFinal,
            String urlInteiroTeor,
            String keywords) {

        String input = String.join("|",
            safe(casa),
            safe(sigla),
            safe(numero),
            safe(ano),
            safe(ementa),
            safe(situacao),
            safe(virouLei),
            safe(idOrigem),
            safe(uriOrigem),
            safe(despachoAtual),
            safe(statusFinal),
            safe(urlInteiroTeor),
            safe(keywords)
        );
        return sha256(input);
    }

    public String generateForFile(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
    /**
     * Calcula o SHA-256 de um arquivo em disco via streaming (sem carregar em mem\u00f3ria).
     */
    public String generateForFilePath(java.nio.file.Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var is = java.nio.file.Files.newInputStream(path)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = is.read(buf)) > 0) {
                    digest.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 n\u00e3o dispon\u00edvel", e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao calcular hash do arquivo: " + path, e);
        }
    }
    public String generateForString(String content) {
        return sha256(content != null ? content : "");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "";
    }
}
