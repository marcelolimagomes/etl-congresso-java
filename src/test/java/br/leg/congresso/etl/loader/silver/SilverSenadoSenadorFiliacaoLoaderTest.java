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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorFiliacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorFiliacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorFiliacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorFiliacaoLoader — insert-if-not-exists por (codigo_senador, codigo_filiacao)")
class SilverSenadoSenadorFiliacaoLoaderTest {

    @Mock
    private SilverSenadoSenadorFiliacaoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorFiliacaoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorFiliacaoDTO.Filiacao filiacao(String codigo, SenadoSenadorFiliacaoDTO.Partido partido) {
        SenadoSenadorFiliacaoDTO.Filiacao f = new SenadoSenadorFiliacaoDTO.Filiacao();
        f.setCodigoFiliacao(codigo);
        f.setDataFiliacao("2005-04-01");
        f.setDataDesfiliacao("2022-12-31");
        f.setPartido(partido);
        return f;
    }

    private SenadoSenadorFiliacaoDTO.Partido partido() {
        SenadoSenadorFiliacaoDTO.Partido p = new SenadoSenadorFiliacaoDTO.Partido();
        p.setCodigoPartido("36842");
        p.setSiglaPartido("PT");
        p.setNomePartido("Partido dos Trabalhadores");
        return p;
    }

    @Test
    @DisplayName("INSERT: filiação nova é salva com todos os campos incluindo sub-objeto partido")
    void inserirFiliacaoNova() {
        SenadoSenadorFiliacaoDTO.Filiacao f = filiacao("200", partido());

        when(repository.existsByCodigoSenadorAndCodigoFiliacao(CODIGO_SENADOR, "200")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(f), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorFiliacao> captor = ArgumentCaptor.forClass(SilverSenadoSenadorFiliacao.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorFiliacao salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoFiliacao()).isEqualTo("200");
        assertThat(salvo.getDataInicioFiliacao()).isEqualTo("2005-04-01");
        assertThat(salvo.getDataTerminoFiliacao()).isEqualTo("2022-12-31");
        assertThat(salvo.getCodigoPartido()).isEqualTo("36842");
        assertThat(salvo.getSiglaPartido()).isEqualTo("PT");
        assertThat(salvo.getNomePartido()).isEqualTo("Partido dos Trabalhadores");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("INSERT: filiação com partido nulo é salva com campos de partido nulos")
    void inserirFiliacaoComPartidoNulo() {
        SenadoSenadorFiliacaoDTO.Filiacao f = filiacao("201", null);

        when(repository.existsByCodigoSenadorAndCodigoFiliacao(CODIGO_SENADOR, "201")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(f), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorFiliacao> captor = ArgumentCaptor.forClass(SilverSenadoSenadorFiliacao.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorFiliacao salvo = captor.getValue();
        assertThat(salvo.getCodigoPartido()).isNull();
        assertThat(salvo.getSiglaPartido()).isNull();
        assertThat(salvo.getNomePartido()).isNull();
    }

    @Test
    @DisplayName("SKIP: filiação já existente não é inserida novamente")
    void ignorarFiliacaoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoFiliacao(CODIGO_SENADOR, "200")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(filiacao("200", partido())), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: filiação com codigoFiliacao nulo é ignorada")
    void ignorarFiliacaoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(filiacao(null, partido())), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(filiacao("200", partido())), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas filiações novas geram múltiplos registros")
    void inserirMultiplasFiliacoes() {
        List<SenadoSenadorFiliacaoDTO.Filiacao> lista = List.of(
                filiacao("1", partido()), filiacao("2", partido()));

        when(repository.existsByCodigoSenadorAndCodigoFiliacao(eq(CODIGO_SENADOR), any())).thenReturn(false);
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
