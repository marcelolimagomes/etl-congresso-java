package br.leg.congresso.etl.transformer;

import br.leg.congresso.etl.domain.enums.TipoProposicao;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Normaliza siglas de proposições para o enum {@link TipoProposicao}.
 * Cobre variações de nomenclatura entre Câmara e Senado.
 */
@Component
public class TipoProposicaoNormalizer {

    /** Siglas aceitas para carga — apenas tipos que podem virar lei */
    private static final Set<String> SIGLAS_ACEITAS = Set.of(
        "PL", "PLP", "MPV", "MP", "PEC", "PDL", "PR", "PLC", "PDS", "PRN"
    );

    /** Mapeamento completo de siglas para tipos normalizados */
    private static final Map<String, TipoProposicao> MAPA_SIGLAS = Map.of(
        "PL",  TipoProposicao.LEI_ORDINARIA,
        "PLC", TipoProposicao.LEI_COMPLEMENTAR,
        "PLP", TipoProposicao.LEI_COMPLEMENTAR,
        "MPV", TipoProposicao.MEDIDA_PROVISORIA,
        "MP",  TipoProposicao.MEDIDA_PROVISORIA,
        "PEC", TipoProposicao.EMENDA_CONSTITUCIONAL,
        "PDL", TipoProposicao.DECRETO_LEGISLATIVO,
        "PDS", TipoProposicao.DECRETO_LEGISLATIVO,
        "PR",  TipoProposicao.RESOLUCAO,
        "PRN", TipoProposicao.RESOLUCAO
    );

    public static TipoProposicao normalizar(String sigla) {
        if (sigla == null || sigla.isBlank()) return TipoProposicao.OUTRO;
        return MAPA_SIGLAS.getOrDefault(sigla.trim().toUpperCase(), TipoProposicao.OUTRO);
    }

    public static boolean isProposicaoAceita(String sigla) {
        if (sigla == null) return false;
        return SIGLAS_ACEITAS.contains(sigla.trim().toUpperCase());
    }
}
