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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorMandato;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorMandatoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorMandatoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorMandatoLoader — insert-if-not-exists por (codigo_senador, codigo_mandato)")
class SilverSenadoSenadorMandatoLoaderTest {

    @Mock
    private SilverSenadoSenadorMandatoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorMandatoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorMandatoDTO.Mandato mandato(String codigo) {
        SenadoSenadorMandatoDTO.Mandato m = new SenadoSenadorMandatoDTO.Mandato();
        m.setCodigoMandato(codigo);
        m.setDescricaoMandato("Senador");
        m.setUfParlamentar("SP");
        m.setDescricaoParticipacao("Titular");
        m.setDataInicio("2019-02-01");
        m.setDataFim("2027-01-31");
        m.setDataDesignacao("2019-01-01");
        m.setDataTermino("2027-01-31");
        m.setEntrouEmExercicio("S");
        m.setDataExercicio("2019-02-01");
        return m;
    }

    @Test
    @DisplayName("INSERT: mandato novo é salvo com todos os campos")
    void inserirMandatoNovo() {
        SenadoSenadorMandatoDTO.Mandato m = mandato("750");

        when(repository.existsByCodigoSenadorAndCodigoMandato(CODIGO_SENADOR, "750")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(m), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorMandato> captor = ArgumentCaptor.forClass(SilverSenadoSenadorMandato.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorMandato salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoMandato()).isEqualTo("750");
        assertThat(salvo.getDescricao()).isEqualTo("Senador");
        assertThat(salvo.getUfMandato()).isEqualTo("SP");
        assertThat(salvo.getParticipacao()).isEqualTo("Titular");
        assertThat(salvo.getDataInicio()).isEqualTo("2019-02-01");
        assertThat(salvo.getDataFim()).isEqualTo("2027-01-31");
        assertThat(salvo.getEntrouExercicio()).isEqualTo("S");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: mandato já existente não é inserido novamente")
    void ignorarMandatoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoMandato(CODIGO_SENADOR, "750")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(mandato("750")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: mandato com codigoMandato nulo é ignorado")
    void ignorarMandatoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(mandato(null)), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(mandato("750")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos mandatos novos geram múltiplos registros")
    void inserirMultiplosMandatos() {
        List<SenadoSenadorMandatoDTO.Mandato> lista = List.of(mandato("1"), mandato("2"), mandato("3"));

        when(repository.existsByCodigoSenadorAndCodigoMandato(eq(CODIGO_SENADOR), any())).thenReturn(false);
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
