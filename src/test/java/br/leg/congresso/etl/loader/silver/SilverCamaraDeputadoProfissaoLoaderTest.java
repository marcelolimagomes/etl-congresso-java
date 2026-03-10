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

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoProfissao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoProfissaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoProfissaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoProfissaoLoader — insert-if-not-exists por (idDeputado, titulo, codTipoProfissao)")
class SilverCamaraDeputadoProfissaoLoaderTest {

    @Mock
    private SilverCamaraDeputadoProfissaoRepository repository;

    @InjectMocks
    private SilverCamaraDeputadoProfissaoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    private CamaraDeputadoProfissaoCSVRow novaRow(String idDeputado, String titulo) {
        CamaraDeputadoProfissaoCSVRow row = new CamaraDeputadoProfissaoCSVRow();
        row.setIdDeputado(idDeputado);
        row.setTitulo(titulo);
        row.setCodTipoProfissao("1");
        row.setDataHora("2023-01-01T00:00");
        return row;
    }

    @Test
    @DisplayName("INSERT: profissão nova é salva")
    void inserirProfissaoNova() {
        CamaraDeputadoProfissaoCSVRow row = novaRow("111", "Advogado");
        when(repository.existsByIdDeputadoAndTituloAndCodTipoProfissao("111", "Advogado", "1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraDeputadoProfissao> captor = ArgumentCaptor
                .forClass(SilverCamaraDeputadoProfissao.class);
        verify(repository).save(captor.capture());
        SilverCamaraDeputadoProfissao salvo = captor.getValue();
        assertThat(salvo.getIdDeputado()).isEqualTo("111");
        assertThat(salvo.getTitulo()).isEqualTo("Advogado");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: profissão já existente não é inserida novamente")
    void ignorarProfissaoExistente() {
        CamaraDeputadoProfissaoCSVRow row = novaRow("111", "Advogado");
        when(repository.existsByIdDeputadoAndTituloAndCodTipoProfissao("111", "Advogado", "1")).thenReturn(true);

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: linha com idDeputado vazio é ignorada")
    void ignorarLinhaComIdDeputadoVazio() {
        CamaraDeputadoProfissaoCSVRow row = novaRow("", "Advogado");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
        verify(repository, never()).existsByIdDeputadoAndTituloAndCodTipoProfissao(any(), any(), any());
    }

    @Test
    @DisplayName("SKIP: linha com titulo vazio é ignorada")
    void ignorarLinhaComTituloVazio() {
        CamaraDeputadoProfissaoCSVRow row = novaRow("111", "");

        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas profissões novas são inseridas corretamente")
    void inserirMultiplosProfissoes() {
        CamaraDeputadoProfissaoCSVRow row1 = novaRow("111", "Advogado");
        CamaraDeputadoProfissaoCSVRow row2 = novaRow("222", "Médico");
        when(repository.existsByIdDeputadoAndTituloAndCodTipoProfissao(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(row1, row2), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero sem chamadas ao repositório")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
