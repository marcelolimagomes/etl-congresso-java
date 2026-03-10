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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorCargo;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorCargoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorCargoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorCargoLoader — insert-if-not-exists por (codigo_senador, codigo_cargo, data_inicio)")
class SilverSenadoSenadorCargoLoaderTest {

    @Mock
    private SilverSenadoSenadorCargoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorCargoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorCargoDTO.Cargo cargo(String codigo, String dataInicio) {
        SenadoSenadorCargoDTO.Cargo c = new SenadoSenadorCargoDTO.Cargo();
        c.setCodigoCargo(codigo);
        c.setDescricaoCargo("Presidente");
        c.setTipoCargo("Mesa Diretora");
        c.setNomeOrgao("Senado Federal");
        c.setDataInicio(dataInicio);
        c.setDataFim("2023-01-31");
        return c;
    }

    @Test
    @DisplayName("INSERT: cargo novo é salvo com todos os campos (chave tripla, nomeOrgao → comissaoOuOrgao)")
    void inserirCargoNovo() {
        SenadoSenadorCargoDTO.Cargo c = cargo("88", "2021-02-01");

        when(repository.existsByCodigoSenadorAndCodigoCargoAndDataInicio(
                CODIGO_SENADOR, "88", "2021-02-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(c), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorCargo> captor = ArgumentCaptor.forClass(SilverSenadoSenadorCargo.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorCargo salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoCargo()).isEqualTo("88");
        assertThat(salvo.getDescricaoCargo()).isEqualTo("Presidente");
        assertThat(salvo.getTipoCargo()).isEqualTo("Mesa Diretora");
        assertThat(salvo.getComissaoOuOrgao()).isEqualTo("Senado Federal");
        assertThat(salvo.getDataInicio()).isEqualTo("2021-02-01");
        assertThat(salvo.getDataFim()).isEqualTo("2023-01-31");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: cargo já existente (mesma chave tripla) não é inserido novamente")
    void ignorarCargoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoCargoAndDataInicio(
                CODIGO_SENADOR, "88", "2021-02-01")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(cargo("88", "2021-02-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("INSERT: mesmo cargo em data diferente gera novo registro")
    void inserirMesmoCargoDataDiferente() {
        when(repository.existsByCodigoSenadorAndCodigoCargoAndDataInicio(
                CODIGO_SENADOR, "88", "2015-02-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(cargo("88", "2015-02-01")), jobId);

        assertThat(resultado).isEqualTo(1);
        verify(repository).save(any());
    }

    @Test
    @DisplayName("SKIP: cargo com codigoCargo nulo é ignorado")
    void ignorarCargoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(cargo(null, "2021-02-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(cargo("88", "2021-02-01")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos cargos novos geram múltiplos registros")
    void inserirMultiplosCargos() {
        List<SenadoSenadorCargoDTO.Cargo> lista = List.of(
                cargo("1", "2019-01-01"), cargo("2", "2021-01-01"));

        when(repository.existsByCodigoSenadorAndCodigoCargoAndDataInicio(
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
