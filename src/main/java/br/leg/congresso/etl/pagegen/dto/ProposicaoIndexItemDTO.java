package br.leg.congresso.etl.pagegen.dto;

/**
 * Item resumido de uma proposição para uso na página-índice estática.
 *
 * @param slugId    identificador slug, ex: {@code camara-2430053}
 * @param titulo    rótulo curto, ex: {@code PL 1441/2024}
 * @param ementa    ementa truncada (máx. 200 chars)
 * @param casaLabel "Câmara dos Deputados" | "Senado Federal"
 * @param situacao  situação atual
 * @param url       caminho relativo, ex: {@code /proposicoes/camara-2430053}
 */
public record ProposicaoIndexItemDTO(
        String slugId,
        String titulo,
        String ementa,
        String casaLabel,
        String situacao,
        String url) {
}
