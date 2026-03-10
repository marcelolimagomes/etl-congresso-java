package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorComissao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorComissaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorComissaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorComissaoLoader — insert-if-not-exists por (codigo_senador, codigo_comissao, data_inicio_participacao)")
class SilverSenadoSenadorComissaoLoaderTest {

    @Mock
    private SilverSenadoSenadorComissaoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorComissaoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorComissaoDTO.Comissao comissao(String codigo, String dataInicio) {
        SenadoSenadorComissaoDTO.Comissao c = new SenadoSenadorComissaoDTO.Comissao();
        c.setCodigoComissao(codigo);
        c.setSiglaComissao("CC" + codigo);
        c.setNomeComissao("Comissão " + codigo);
        c.setDescricaoParticipacao("Titular");
        c.setDataInicio(dataInicio);
        c.setDataFim("2024-12-31");
        c.setIndicadorAtividade("S");
        return c;
    }

    @Test
    @DisplayName("INSERT: participação em comissão nova é salva com todos os campos (chave tripla)")
    void inserirComissaoNova() {
        SenadoSenadorComissaoDTO.Comissao c = comissao("55", "2023-03-01");

        when(repository.existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
                CODIGO_SENADOR, "55", "2023-03-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(c), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorComissao> captor = ArgumentCaptor.forClass(SilverSenadoSenadorComissao.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorComissao salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoComissao()).isEqualTo("55");
        assertThat(salvo.getSiglaComissao()).isEqualTo("CC55");
        assertThat(salvo.getNomeComissao()).isEqualTo("Comissão 55");
        assertThat(salvo.getCargo()).isEqualTo("Titular");
        assertThat(salvo.getDataInicioParticipacao()).isEqualTo("2023-03-01");
        assertThat(salvo.getDataTerminoParticipacao()).isEqualTo("2024-12-31");
        assertThat(salvo.getAtivo()).isEqualTo("S");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: participação já existente (mesma chave tripla) não é inserida novamente")
    void ignorarComissaoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
                CODIGO_SENADOR, "55", "2023-03-01")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(comissao("55", "2023-03-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("INSERT: mesma comissão em data diferente gera novo registro")
    void inserirMesmaComissaoDataDiferente() {
        when(repository.existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
                CODIGO_SENADOR, "55", "2019-02-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(comissao("55", "2019-02-01")), jobId);

        assertThat(resultado).isEqualTo(1);
        verify(repository).save(any());
    }

    @Test
    @DisplayName("SKIP: comissão com codigoComissao nulo é ignorada")
    void ignorarComissaoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(comissao(null, "2023-03-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(comissao("55", "2023-03-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas participações novas geram múltiplos registros")
    void inserirMultiplasComissoes() {
        List<SenadoSenadorComissaoDTO.Comissao> lista = List.of(
                comissao("1", "2020-01-01"), comissao("2", "2021-01-01"));

        when(repository.existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
                eq(CODIGO_SENADOR), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, lista, jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(CODIGO_SENADOR, Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("NULL: lista nula retorna zero sem lançar exceção")
    void listaNulaRetornaZero() {
        int resultado = loader.carregar(CODIGO_SENADOR, null, jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
