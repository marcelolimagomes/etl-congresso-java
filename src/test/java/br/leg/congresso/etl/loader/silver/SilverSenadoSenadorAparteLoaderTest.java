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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAparte;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorAparteDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorAparteRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorAparteLoader — insert-if-not-exists por (codigo_senador, codigo_aparte)")
class SilverSenadoSenadorAparteLoaderTest {

    @Mock
    private SilverSenadoSenadorAparteRepository repository;

    @InjectMocks
    private SilverSenadoSenadorAparteLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorAparteDTO.Aparte aparte(String codigo) {
        SenadoSenadorAparteDTO.Aparte a = new SenadoSenadorAparteDTO.Aparte();
        a.setCodigoAparte(codigo);
        a.setCodigoPronunciamentoPrincipal("9999");
        a.setCodigoSessao("20230601T100000");
        a.setDataPronunciamento("2023-06-01");
        a.setCasa("SF");
        a.setTextoAparte("Texto do aparte...");
        a.setUrlVideo("https://www.senado.leg.br/video/ap123");
        return a;
    }

    @Test
    @DisplayName("INSERT: aparte novo é salvo com todos os campos (codigoPronunciamentoPrincipal → codigoDiscursoPrincipal)")
    void inserirAparteNovo() {
        SenadoSenadorAparteDTO.Aparte a = aparte("500");

        when(repository.existsByCodigoSenadorAndCodigoAparte(CODIGO_SENADOR, "500")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(a), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorAparte> captor = ArgumentCaptor.forClass(SilverSenadoSenadorAparte.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorAparte salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoAparte()).isEqualTo("500");
        assertThat(salvo.getCodigoDiscursoPrincipal()).isEqualTo("9999");
        assertThat(salvo.getCodigoSessao()).isEqualTo("20230601T100000");
        assertThat(salvo.getDataPronunciamento()).isEqualTo("2023-06-01");
        assertThat(salvo.getCasa()).isEqualTo("SF");
        assertThat(salvo.getTextoAparte()).isEqualTo("Texto do aparte...");
        assertThat(salvo.getUrlVideo()).isEqualTo("https://www.senado.leg.br/video/ap123");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: aparte já existente não é inserido novamente")
    void ignorarAparteExistente() {
        when(repository.existsByCodigoSenadorAndCodigoAparte(CODIGO_SENADOR, "500")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(aparte("500")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: aparte com codigoAparte nulo é ignorado")
    void ignorarAparteComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(aparte(null)), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(aparte("500")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos apartes novos geram múltiplos registros")
    void inserirMultiplosApartes() {
        List<SenadoSenadorAparteDTO.Aparte> lista = List.of(aparte("1"), aparte("2"), aparte("3"));

        when(repository.existsByCodigoSenadorAndCodigoAparte(eq(CODIGO_SENADOR), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, lista, jobId);

        assertThat(resultado).isEqualTo(3);
        verify(repository, times(3)).save(any());
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
