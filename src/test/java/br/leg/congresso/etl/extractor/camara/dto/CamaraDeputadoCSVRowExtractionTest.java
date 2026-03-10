package br.leg.congresso.etl.extractor.camara.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.leg.congresso.etl.extractor.camara.CamaraCSVExtractor;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;

/**
 * Testes unitários para extração do CSV deputados.csv via
 * {@link CamaraCSVExtractor#extractRowsInChunks}.
 */
@DisplayName("CamaraDeputadoCSVRow — extração genérica do CSV deputados.csv")
class CamaraDeputadoCSVRowExtractionTest {

    @TempDir
    Path tmpDir;

    private CamaraCSVExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CamaraCSVExtractor(mock(CamaraProposicaoMapper.class));
    }

    private Path csvWith(String content) throws IOException {
        Path csv = tmpDir.resolve("deputados.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    // Formato real do CSV da Câmara (verificado 2025)
    private static final String HEADER = "uri;nome;idLegislaturaInicial;idLegislaturaFinal;" +
            "nomeCivil;cpf;siglaSexo;urlRedeSocial;urlWebsite;" +
            "dataNascimento;dataFalecimento;ufNascimento;municipioNascimento\n";

    @Test
    @DisplayName("deve extrair linha única com todos os campos mapeados corretamente")
    void extrairLinhaUnica() throws IOException {
        String csv = HEADER +
                "https://dadosabertos.camara.leg.br/api/v2/deputados/123;João Silva;50;57;" +
                "JOÃO DA SILVA;12345678900;M;;https://joao.com.br;" +
                "1970-05-15;;SP;São Paulo\n";

        List<CamaraDeputadoCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(csv), CamaraDeputadoCSVRow.class, coletado::addAll);

        assertThat(total).isEqualTo(1);
        assertThat(coletado).hasSize(1);

        CamaraDeputadoCSVRow row = coletado.get(0);
        assertThat(row.getUri()).contains("/123");
        assertThat(row.getNome()).isEqualTo("João Silva");
        assertThat(row.getNomeCivil()).isEqualTo("JOÃO DA SILVA");
        assertThat(row.getSiglaSexo()).isEqualTo("M");
        assertThat(row.getDataNascimento()).isEqualTo("1970-05-15");
        assertThat(row.getUfNascimento()).isEqualTo("SP");
        assertThat(row.getIdLegislaturaInicial()).isEqualTo("50");
        assertThat(row.getIdLegislaturaFinal()).isEqualTo("57");
    }

    @Test
    @DisplayName("deve extrair múltiplas linhas")
    void extrairMultiplasLinhas() throws IOException {
        String csv = HEADER +
                "https://dadosabertos.camara.leg.br/api/v2/deputados/100;Fulano;55;57;" +
                "FULANO DE TAL;111;M;;https://fulano.com;1980-01-01;;RJ;Rio de Janeiro\n" +
                "https://dadosabertos.camara.leg.br/api/v2/deputados/200;Beltrana;56;57;" +
                "BELTRANA DA COSTA;222;F;;https://beltrana.com;1985-06-20;;MG;Belo Horizonte\n";

        List<CamaraDeputadoCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(csv), CamaraDeputadoCSVRow.class, coletado::addAll);

        assertThat(total).isEqualTo(2);
        assertThat(coletado).hasSize(2);
        assertThat(coletado.get(0).getUri()).endsWith("/100");
        assertThat(coletado.get(1).getUri()).endsWith("/200");
        assertThat(coletado.get(1).getSiglaSexo()).isEqualTo("F");
    }

    @Test
    @DisplayName("deve remover BOM e mapear corretamente")
    void extrairComBOM() throws IOException {
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        String csvBody = HEADER +
                "https://dadosabertos.camara.leg.br/api/v2/deputados/999;Maria Dep;56;57;" +
                "MARIA SILVA;333;F;;https://maria.com;1990-03-10;;BA;Salvador\n";
        byte[] csvBytes = csvBody.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(csvBytes, 0, withBom, bom.length, csvBytes.length);

        Path csv = tmpDir.resolve("deputados_bom.csv");
        Files.write(csv, withBom);

        List<CamaraDeputadoCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csv, CamaraDeputadoCSVRow.class, coletado::addAll);

        assertThat(total).isEqualTo(1);
        assertThat(coletado.get(0).getUri()).endsWith("/999");
    }

    @Test
    @DisplayName("deve retornar zero para CSV com apenas cabeçalho")
    void csvSomenteComCabecalho() throws IOException {
        List<CamaraDeputadoCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(HEADER), CamaraDeputadoCSVRow.class, coletado::addAll);

        assertThat(total).isEqualTo(0);
        assertThat(coletado).isEmpty();
    }
}
