package br.leg.congresso.etl.pagegen.dto;

/**
 * Item resumido de um parlamentar para uso na página-índice estática.
 *
 * @param slugId       identificador slug, ex: {@code camara-2430}
 * @param nome         nome parlamentar
 * @param partido      sigla do partido
 * @param uf           sigla da UF
 * @param casa         identificador da casa: {@code camara} | {@code senado}
 * @param casaLabel    rótulo da casa: {@code Câmara dos Deputados} | {@code Senado Federal}
 * @param urlFoto      URL da foto oficial (pode ser null)
 * @param participacao {@code Titular} | {@code Suplente} (senadores) ou null (deputados)
 * @param url          caminho relativo, ex: {@code /stat-parlamentares/camara-2430/}
 */
public record ParlamentarIndexItemDTO(
        String slugId,
        String nome,
        String partido,
        String uf,
        String casa,
        String casaLabel,
        String urlFoto,
        String participacao,
        String url) {
}