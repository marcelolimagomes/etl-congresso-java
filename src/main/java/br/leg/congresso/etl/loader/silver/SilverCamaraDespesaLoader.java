package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDespesa;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDespesaCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDespesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega despesas CEAP da Câmara na camada Silver (silver.camara_despesa).
 *
 * Princípio Silver: passthrough fiel ao CSV Ano-{ano}.csv (Cota Parlamentar).
 * Deduplicação por (camara_deputado_id, cod_documento, num_documento, parcela).
 * Registros já existentes são ignorados (insert-if-not-exists).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDespesaLoader {

    private final SilverCamaraDespesaRepository repository;

    /**
     * Persiste despesas CEAP, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo Ano-{ano}.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDespesaCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDespesaCSVRow row : rows) {
            if (row.getIdeCadastro() == null || row.getIdeCadastro().isBlank()) {
                continue;
            }

            if (!repository.existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
                    row.getIdeCadastro(),
                    row.getIdeDocumento(),
                    row.getTxtNumero(),
                    row.getNumParcela())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara despesas CEAP: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDespesa rowToEntity(CamaraDespesaCSVRow row, UUID jobId) {
        return SilverCamaraDespesa.builder()
                .etlJobId(jobId)
                .camaraDeputadoId(row.getIdeCadastro())
                .ano(row.getNumAno())
                .mes(row.getNumMes())
                .tipoDespesa(row.getTxtDescricao())
                .codDocumento(row.getIdeDocumento())
                .tipoDocumento(row.getTxtDescricaoEspecificacao())
                .codTipoDocumento(row.getIndTipoDocumento())
                .dataDocumento(row.getDatEmissao())
                .numDocumento(row.getTxtNumero())
                .parcela(row.getNumParcela())
                .valorDocumento(row.getVlrDocumento())
                .valorGlosa(row.getVlrGlosa())
                .valorLiquido(row.getVlrLiquido())
                .nomeFornecedor(row.getTxtFornecedor())
                .cnpjCpfFornecedor(row.getTxtCNPJCPF())
                .numRessarcimento(row.getNumRessarcimento())
                .urlDocumento(row.getUrlDocumento())
                .codLote(row.getNumLote())
                .build();
    }
}
