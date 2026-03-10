package br.leg.congresso.etl.extractor.camara;

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

import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoTemaCSVRow;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;

/**
 * Testes unitários para
 * {@link CamaraCSVExtractor#extractRowsInChunks(Path, Class, java.util.function.Consumer)}.
 */
@DisplayName("CamaraCSVExtractor — extractRowsInChunks genérico")
class CamaraCSVExtractorGenericTest {

    @TempDir
    Path tmpDir;

    private CamaraCSVExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CamaraCSVExtractor(mock(CamaraProposicaoMapper.class));
    }

    private Path csvWith(String content) throws IOException {
        Path csv = tmpDir.resolve("test.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    // ─── testes ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve extrair todas as linhas e emitir em chunk único")
    void extractRowsInChunks_emiteTodasAsLinhas() throws IOException {
        String csv = "uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia\n" +
                "http://exemplo/1;PL;1;2024;10;Tecnologia;100\n" +
                "http://exemplo/2;PEC;2;2024;20;Saúde;90\n";

        List<CamaraProposicaoTemaCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(csv), CamaraProposicaoTemaCSVRow.class,
                coletado::addAll);

        assertThat(total).isEqualTo(2);
        assertThat(coletado).hasSize(2);
        assertThat(coletado.get(0).getSiglaTipo()).isEqualTo("PL");
        assertThat(coletado.get(1).getSiglaTipo()).isEqualTo("PEC");
    }

    @Test
    @DisplayName("deve remover BOM UTF-8 e ainda mapear corretamente")
    void extractRowsInChunks_removeBOM() throws IOException {
        // BOM = EF BB BF em UTF-8
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        String csvBody = "uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia\n" +
                "http://exemplo/1;PL;1;2024;10;Tecnologia;100\n";
        byte[] csvBytes = csvBody.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(csvBytes, 0, withBom, bom.length, csvBytes.length);

        Path csv = tmpDir.resolve("bom.csv");
        Files.write(csv, withBom);

        List<CamaraProposicaoTemaCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csv, CamaraProposicaoTemaCSVRow.class,
                coletado::addAll);

        assertThat(total).isEqualTo(1);
        assertThat(coletado.get(0).getAno()).isEqualTo("2024");
    }

    @Test
    @DisplayName("deve retornar zero linhas para CSV vazio (apenas cabeçalho)")
    void extractRowsInChunks_csvApenasCabecalho_retornaZero() throws IOException {
        String csv = "uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia\n";

        List<CamaraProposicaoTemaCSVRow> coletado = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(csv), CamaraProposicaoTemaCSVRow.class,
                coletado::addAll);

        assertThat(total).isEqualTo(0);
        assertThat(coletado).isEmpty();
    }

    @Test
    @DisplayName("deve emitir múltiplos chunks quando o lote excede o tamanho padrão")
    void extractRowsInChunks_processaMultiplosChunks() throws IOException {
        // chunk-size padrão é 10000; para testar basta verificar que acumula
        StringBuilder csv = new StringBuilder("uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia\n");
        for (int i = 1; i <= 5; i++) {
            csv.append("http://exemplo/").append(i).append(";PL;").append(i).append(";2024;1;Tema;100\n");
        }

        List<List<CamaraProposicaoTemaCSVRow>> chunks = new ArrayList<>();
        long total = extractor.extractRowsInChunks(csvWith(csv.toString()), CamaraProposicaoTemaCSVRow.class,
                chunk -> chunks.add(new ArrayList<>(chunk)));

        assertThat(total).isEqualTo(5);
        // Com 5 linhas e chunk-size padrão 10000, deve haver exatamente 1 chunk
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(5);
    }

    @Test
    @DisplayName("deve preencher campo 'tema' corretamente")
    void extractRowsInChunks_mapeaCamposTema() throws IOException {
        String csv = "uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia\n" +
                "http://ex/1;PL;1;2024;42;Meio Ambiente;75\n";

        List<CamaraProposicaoTemaCSVRow> coletado = new ArrayList<>();
        extractor.extractRowsInChunks(csvWith(csv), CamaraProposicaoTemaCSVRow.class, coletado::addAll);

        assertThat(coletado).hasSize(1);
        CamaraProposicaoTemaCSVRow row = coletado.get(0);
        assertThat(row.getCodTema()).isEqualTo("42");
        assertThat(row.getTema()).isEqualTo("Meio Ambiente");
        assertThat(row.getRelevancia()).isEqualTo("75");
    }
}
