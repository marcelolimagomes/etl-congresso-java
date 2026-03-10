package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de despesas CEAP da Câmara dos Deputados.
 * URL: http://www.camara.leg.br/cotas/Ano-{ano}.csv.zip
 * Separador: ponto-e-vírgula (;).
 *
 * Os nomes dos campos seguem exatamente os cabeçalhos do CSV da Câmara.
 * Mapeamento para coluna Silver: veja SilverCamaraDespesa.
 */
@Data
public class CamaraDespesaCSVRow {

    /** ideCadastro → camara_deputado_id */
    @CsvBindByName(column = "ideCadastro")
    private String ideCadastro;

    /** numAno → ano */
    @CsvBindByName(column = "numAno")
    private String numAno;

    /** numMes → mes */
    @CsvBindByName(column = "numMes")
    private String numMes;

    /** txtDescricao → tipo_despesa */
    @CsvBindByName(column = "txtDescricao")
    private String txtDescricao;

    /** ideDocumento → cod_documento */
    @CsvBindByName(column = "ideDocumento")
    private String ideDocumento;

    /** txtDescricaoEspecificacao → tipo_documento */
    @CsvBindByName(column = "txtDescricaoEspecificacao")
    private String txtDescricaoEspecificacao;

    /** indTipoDocumento → cod_tipo_documento */
    @CsvBindByName(column = "indTipoDocumento")
    private String indTipoDocumento;

    /** datEmissao → data_documento */
    @CsvBindByName(column = "datEmissao")
    private String datEmissao;

    /** txtNumero → num_documento */
    @CsvBindByName(column = "txtNumero")
    private String txtNumero;

    /** numParcela → parcela */
    @CsvBindByName(column = "numParcela")
    private String numParcela;

    /** vlrDocumento → valor_documento */
    @CsvBindByName(column = "vlrDocumento")
    private String vlrDocumento;

    /** vlrGlosa → valor_glosa */
    @CsvBindByName(column = "vlrGlosa")
    private String vlrGlosa;

    /** vlrLiquido → valor_liquido */
    @CsvBindByName(column = "vlrLiquido")
    private String vlrLiquido;

    /** txtFornecedor → nome_fornecedor */
    @CsvBindByName(column = "txtFornecedor")
    private String txtFornecedor;

    /** txtCNPJCPF → cnpj_cpf_fornecedor */
    @CsvBindByName(column = "txtCNPJCPF")
    private String txtCNPJCPF;

    /** numRessarcimento → num_ressarcimento */
    @CsvBindByName(column = "numRessarcimento")
    private String numRessarcimento;

    /** urlDocumento → url_documento */
    @CsvBindByName(column = "urlDocumento")
    private String urlDocumento;

    /** numLote → cod_lote */
    @CsvBindByName(column = "numLote")
    private String numLote;
}
