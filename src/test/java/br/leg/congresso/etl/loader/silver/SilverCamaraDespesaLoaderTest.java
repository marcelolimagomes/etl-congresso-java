package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverCamaraDespesa;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDespesaCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDespesaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDespesaLoader — insert-if-not-exists por (camaraDeputadoId, codDocumento, numDocumento, parcela)")
class SilverCamaraDespesaLoaderTest {

    @Mock
    private SilverCamaraDespesaRepository repository;

    @InjectMocks
    private SilverCamaraDespesaLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDespesaCSVRow novaDespesa(String ideCadastro, String ideDocumento,
            String txtNumero, String numParcela) {
        CamaraDespesaCSVRow row = new CamaraDespesaCSVRow();
        row.setIdeCadastro(ideCadastro);
        row.setIdeDocumento(ideDocumento);
        row.setTxtNumero(txtNumero);
        row.setNumParcela(numParcela);
        row.setNumAno("2024");
        row.setNumMes("3");
        row.setTxtDescricao("COMBUSTÍVEIS E LUBRIFICANTES");
        row.setTxtDescricaoEspecificacao("Nota Fiscal");
        row.setIndTipoDocumento("0");
        row.setDatEmissao("2024-03-15");
        row.setVlrDocumento("250,00");
        row.setVlrGlosa("0,00");
        row.setVlrLiquido("250,00");
        row.setTxtFornecedor("POSTO GASOLINA LTDA");
        row.setTxtCNPJCPF("12345678000195");
        row.setNumRessarcimento("0");
        row.setUrlDocumento("https://camara.leg.br/cota-parlamentar/nota-fiscal/" + ideDocumento);
        row.setNumLote("1");
        return row;
    }

    @Test
    @DisplayName("INSERT: despesa nova é salva com todos os campos mapeados")
    void inserirDespesaNova() {
        CamaraDespesaCSVRow row = novaDespesa("11111", "99001", "NF-001", "0");
        when(repository.existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
                "11111", "99001", "NF-001", "0")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDespesa> captor = ArgumentCaptor.forClass(SilverCamaraDespesa.class);
        verify(repository).save(captor.capture());
        SilverCamaraDespesa salvo = captor.getValue();
        assertThat(salvo.getCamaraDeputadoId()).isEqualTo("11111");
        assertThat(salvo.getCodDocumento()).isEqualTo("99001");
        assertThat(salvo.getNumDocumento()).isEqualTo("NF-001");
        assertThat(salvo.getParcela()).isEqualTo("0");
        assertThat(salvo.getAno()).isEqualTo("2024");
        assertThat(salvo.getMes()).isEqualTo("3");
        assertThat(salvo.getTipoDespesa()).isEqualTo("COMBUSTÍVEIS E LUBRIFICANTES");
        assertThat(salvo.getValorLiquido()).isEqualTo("250,00");
        assertThat(salvo.getNomeFornecedor()).isEqualTo("POSTO GASOLINA LTDA");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("SKIP: despesa já existente não é inserida")
    void ignorarDespesaExistente() {
        CamaraDespesaCSVRow row = novaDespesa("11111", "99001", "NF-001", "0");
        when(repository.existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
                any(), any(), any(), any())).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com ideCadastro vazio é ignorada")
    void ignorarLinhaComIdeCadastroVazio() {
        CamaraDespesaCSVRow row = novaDespesa("", "99001", "NF-001", "0");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com ideCadastro nulo é ignorada")
    void ignorarLinhaComIdeCadastroNulo() {
        CamaraDespesaCSVRow row = novaDespesa(null, "99001", "NF-001", "0");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas despesas inseridas")
    void inserirMultiplasDespesas() {
        CamaraDespesaCSVRow row1 = novaDespesa("11111", "99001", "NF-001", "0");
        CamaraDespesaCSVRow row2 = novaDespesa("22222", "99002", "NF-002", "0");
        when(repository.existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
                any(), any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row1, row2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("BATCH: parcelas distintas do mesmo documento são inseridas separadamente")
    void inserirParcelasDistintas() {
        CamaraDespesaCSVRow row1 = novaDespesa("11111", "99001", "NF-001", "1");
        CamaraDespesaCSVRow row2 = novaDespesa("11111", "99001", "NF-001", "2");
        when(repository.existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
                any(), any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row1, row2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
