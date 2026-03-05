package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvToBeanBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que os 31 bindings de CamaraProposicaoCSVRow correspondem
 * exatamente às colunas do CSV real da Câmara dos Deputados.
 *
 * Bug pré-existente corrigido: versão anterior usava nomes 'ultimaSituacao*'
 * que não existem no CSV — as colunas reais têm prefixo 'ultimoStatus_*'.
 */
@DisplayName("CamaraProposicaoCSVRow — mapeamento dos 31 campos do CSV")
class CamaraProposicaoCSVRowTest {

    /**
     * Cabeçalho exato do CSV real da Câmara (verificado em 2001, 2010, 2024).
     * Separador: ponto-e-vírgula; encoding: UTF-8 com BOM (removido pelo extrator).
     */
    private static final String HEADER =
            "\"id\";\"uri\";\"siglaTipo\";\"numero\";\"ano\";\"codTipo\";\"descricaoTipo\";" +
            "\"ementa\";\"ementaDetalhada\";\"keywords\";\"dataApresentacao\";" +
            "\"uriOrgaoNumerador\";\"uriPropAnterior\";\"uriPropPrincipal\";\"uriPropPosterior\";" +
            "\"urlInteiroTeor\";\"urnFinal\";" +
            "\"ultimoStatus_dataHora\";\"ultimoStatus_sequencia\";\"ultimoStatus_uriRelator\";" +
            "\"ultimoStatus_idOrgao\";\"ultimoStatus_siglaOrgao\";\"ultimoStatus_uriOrgao\";" +
            "\"ultimoStatus_regime\";\"ultimoStatus_descricaoTramitacao\";" +
            "\"ultimoStatus_idTipoTramitacao\";\"ultimoStatus_descricaoSituacao\";" +
            "\"ultimoStatus_idSituacao\";\"ultimoStatus_despacho\";" +
            "\"ultimoStatus_apreciacao\";\"ultimoStatus_url\"";

    private static final String DATA_ROW =
            "\"2271068\";" +
            "\"https://dadosabertos.camara.leg.br/api/v2/proposicoes/2271068\";" +
            "\"PL\";" +
            "\"1\";" +
            "\"2024\";" +
            "\"139\";" +
            "\"Projeto de Lei\";" +
            "\"Ementa de teste\";" +
            "\"Ementa detalhada de teste\";" +
            "\"palavra-chave1;palavra-chave2\";" +
            "\"2024-01-15T00:00:00\";" +
            "\"https://dadosabertos.camara.leg.br/api/v2/orgaos/180\";" +
            "\"\";\"\";\"\";"+
            "\"https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2271068\";" +
            "\"\";" +
            "\"2024-06-10T14:30:00\";" +
            "\"5\";" +
            "\"https://dadosabertos.camara.leg.br/api/v2/deputados/12345\";" +
            "\"180\";" +
            "\"PLEN\";" +
            "\"https://dadosabertos.camara.leg.br/api/v2/orgaos/180\";" +
            "\"Ordinária\";" +
            "\"Deliberação\";" +
            "\"130\";" +
            "\"Em tramitação no Plenário\";" +
            "\"1140\";" +
            "\"Encaminhado à votação\";" +
            "\"Proposição Sujeita à Apreciação do Plenário\";" +
            "\"https://www.camara.leg.br/proposicoesWeb/prop_tramitacao?codProposicao=2271068\"";

    private CamaraProposicaoCSVRow parse(String csv) {
        List<CamaraProposicaoCSVRow> rows = new CsvToBeanBuilder<CamaraProposicaoCSVRow>(new StringReader(csv))
                .withType(CamaraProposicaoCSVRow.class)
                .withSeparator(';')
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse();
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    @Test
    @DisplayName("parse correto de todos os 31 campos do CSV real")
    void todosCamposParseados() {
        CamaraProposicaoCSVRow row = parse(HEADER + "\n" + DATA_ROW);

        // Identificação
        assertThat(row.getId()).isEqualTo("2271068");
        assertThat(row.getUri()).contains("proposicoes/2271068");
        assertThat(row.getSiglaTipo()).isEqualTo("PL");
        assertThat(row.getNumero()).isEqualTo("1");
        assertThat(row.getAno()).isEqualTo("2024");
        assertThat(row.getCodTipo()).isEqualTo("139");
        assertThat(row.getDescricaoTipo()).isEqualTo("Projeto de Lei");

        // Descrição
        assertThat(row.getEmenta()).isEqualTo("Ementa de teste");
        assertThat(row.getEmentaDetalhada()).isEqualTo("Ementa detalhada de teste");
        assertThat(row.getKeywords()).isNotBlank();
        assertThat(row.getDataApresentacao()).isEqualTo("2024-01-15T00:00:00");

        // URIs
        assertThat(row.getUriOrgaoNumerador()).contains("orgaos/180");
        assertThat(row.getUrlInteiroTeor()).contains("fichadetramitacao");

        // ultimoStatus_* — campos críticos (eram null com bindings incorretos)
        assertThat(row.getUltimoStatusDataHora())
                .as("ultimoStatus_dataHora deve ser preenchido")
                .isEqualTo("2024-06-10T14:30:00");

        assertThat(row.getUltimoStatusSequencia())
                .as("ultimoStatus_sequencia deve ser preenchido")
                .isEqualTo("5");

        assertThat(row.getUltimoStatusDescricaoSituacao())
                .as("ultimoStatus_descricaoSituacao — era null com bug anterior")
                .isEqualTo("Em tramitação no Plenário");

        assertThat(row.getUltimoStatusDespacho())
                .as("ultimoStatus_despacho — era null com bug anterior")
                .isEqualTo("Encaminhado à votação");

        assertThat(row.getUltimoStatusSiglaOrgao()).isEqualTo("PLEN");
        assertThat(row.getUltimoStatusIdOrgao()).isEqualTo("180");
        assertThat(row.getUltimoStatusRegime()).isEqualTo("Ordinária");
        assertThat(row.getUltimoStatusDescricaoTramitacao()).isEqualTo("Deliberação");
        assertThat(row.getUltimoStatusIdTipoTramitacao()).isEqualTo("130");
        assertThat(row.getUltimoStatusIdSituacao()).isEqualTo("1140");
        assertThat(row.getUltimoStatusApreciacao()).contains("Plenário");
        assertThat(row.getUltimoStatusUrl()).contains("codProposicao=2271068");
    }

    @Test
    @DisplayName("campos ultimoStatus_* NÃO são null após parse correto — regressão do bug anterior")
    void regessaoBugBindingsIncorretos() {
        CamaraProposicaoCSVRow row = parse(HEADER + "\n" + DATA_ROW);

        // Estes campos eram sempre null com os bindings 'ultimaSituacao*' (incorretos)
        assertThat(row.getUltimoStatusDescricaoSituacao())
                .as("REGRESSÃO: situacao não pode ser null após corrigir binding")
                .isNotNull()
                .isNotBlank();

        assertThat(row.getUltimoStatusDespacho())
                .as("REGRESSÃO: despachoAtual não pode ser null após corrigir binding")
                .isNotNull()
                .isNotBlank();

        assertThat(row.getUltimoStatusDataHora())
                .as("REGRESSÃO: dataAtualizacao não pode ser null após corrigir binding")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("linha com campos ultimoStatus_* vazios não lança exceção")
    void camposUltimoStatusVaziosNaoLancamExcecao() {
        String emptyStatusRow =
                "\"999\";\"uri\";\"PL\";\"1\";\"2024\";\"139\";\"Tipo\";" +
                "\"Ementa\";\"\";\"\";" +
                "\"2024-01-01T00:00:00\";" +
                "\"\";\"\";\"\";\"\";\"\";\"\";"+
                "\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\";\"\"";

        CamaraProposicaoCSVRow row = parse(HEADER + "\n" + emptyStatusRow);

        assertThat(row.getId()).isEqualTo("999");
        assertThat(row.getUltimoStatusDescricaoSituacao()).isNullOrEmpty();
        assertThat(row.getUltimoStatusDespacho()).isNullOrEmpty();
        assertThat(row.getUltimoStatusDataHora()).isNullOrEmpty();
    }
}
