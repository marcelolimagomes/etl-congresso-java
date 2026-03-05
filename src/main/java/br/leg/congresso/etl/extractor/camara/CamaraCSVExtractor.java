package br.leg.congresso.etl.extractor.camara;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoCSVRow;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Extrai e transforma proposições a partir dos arquivos CSV da Câmara dos Deputados.
 * Processa o CSV em lotes (chunks) para controle de memória.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamaraCSVExtractor {

    private final CamaraProposicaoMapper mapper;

    @Value("${etl.camara.chunk-size:10000}")
    private int chunkSize;

    /**
     * Lê o CSV em chunks emitindo os CSVRows brutos (sem mapeamento para domínio).
     * Usado pela camada Silver para manter passthrough fiel à fonte.
     *
     * @param csvPath       caminho do arquivo CSV
     * @param rowConsumer   função que receberá cada chunk de CSVRows
     * @return total de linhas lidas (incluindo filtradas)
     */
    public long extractRawInChunks(Path csvPath, Consumer<List<CamaraProposicaoCSVRow>> rowConsumer) throws IOException {
        log.info("[Silver] Iniciando extração raw CSV: {}", csvPath);

        long totalLidas = 0;
        long totalAceitas = 0;
        long totalFiltradas = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {

            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != '\uFEFF') {
                reader.reset();
            }

            Iterator<CamaraProposicaoCSVRow> iterator = new CsvToBeanBuilder<CamaraProposicaoCSVRow>(reader)
                    .withType(CamaraProposicaoCSVRow.class)
                    .withSeparator(';')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false)
                    .build()
                    .iterator();

            List<CamaraProposicaoCSVRow> chunk = new ArrayList<>(chunkSize);

            while (iterator.hasNext()) {
                totalLidas++;
                CamaraProposicaoCSVRow row = iterator.next();

                if (!TipoProposicaoNormalizer.isProposicaoAceita(row.getSiglaTipo())) {
                    totalFiltradas++;
                    continue;
                }

                chunk.add(row);
                totalAceitas++;

                if (chunk.size() >= chunkSize) {
                    rowConsumer.accept(new ArrayList<>(chunk));
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                rowConsumer.accept(chunk);
            }
        }

        log.info("[Silver] Extração raw concluída: {} lidas, {} aceitas, {} filtradas", totalLidas, totalAceitas, totalFiltradas);
        return totalLidas;
    }

    /**
     * Lê o CSV em chunks e invoca o consumer para cada lote.
     * Filtra apenas os tipos de proposição aceitos.
     *
     * @param csvPath   caminho do arquivo CSV
     * @param consumer  função que receberá cada chunk de proposições
     * @return total de linhas lidas (incluindo filtradas)
     */
    public long extractInChunks(Path csvPath, Consumer<List<Proposicao>> consumer) throws IOException {
        log.info("Iniciando extração CSV: {}", csvPath);

        long totalLidas = 0;
        long totalAceitas = 0;
        long totalFiltradas = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {

            // Remove BOM UTF-8, quando presente, para não quebrar o mapeamento da primeira coluna (id)
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != '\uFEFF') {
                reader.reset();
            }

            // Configura o parser OpenCSV
            Iterator<CamaraProposicaoCSVRow> iterator = new CsvToBeanBuilder<CamaraProposicaoCSVRow>(reader)
                    .withType(CamaraProposicaoCSVRow.class)
                    .withSeparator(';')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false)
                    .build()
                    .iterator();

            List<Proposicao> chunk = new ArrayList<>(chunkSize);

            while (iterator.hasNext()) {
                totalLidas++;
                CamaraProposicaoCSVRow row = iterator.next();

                // Filtra por tipo de proposição aceita
                if (!TipoProposicaoNormalizer.isProposicaoAceita(row.getSiglaTipo())) {
                    totalFiltradas++;
                    continue;
                }

                try {
                    Proposicao proposicao = mapper.csvRowToProposicao(row);
                    if (proposicao.getNumero() != null && proposicao.getAno() != null) {
                        chunk.add(proposicao);
                        totalAceitas++;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao mapear linha CSV (linha {}): {} — {}", totalLidas, row, e.getMessage());
                }

                // Emite chunk quando atinge tamanho configurado
                if (chunk.size() >= chunkSize) {
                    log.debug("Emitindo chunk de {} proposições (total lidas: {})", chunk.size(), totalLidas);
                    consumer.accept(new ArrayList<>(chunk));
                    chunk.clear();
                }
            }

            // Emite último chunk parcial
            if (!chunk.isEmpty()) {
                log.debug("Emitindo último chunk de {} proposições", chunk.size());
                consumer.accept(chunk);
            }
        }

        log.info("Extração CSV concluída: {} lidas, {} aceitas, {} filtradas", totalLidas, totalAceitas, totalFiltradas);
        return totalLidas;
    }
}
