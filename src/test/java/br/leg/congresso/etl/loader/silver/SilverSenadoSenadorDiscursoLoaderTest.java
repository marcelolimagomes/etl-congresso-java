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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorDiscurso;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDiscursoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorDiscursoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorDiscursoLoader — insert-if-not-exists por (codigo_senador, codigo_discurso)")
class SilverSenadoSenadorDiscursoLoaderTest {

    @Mock
    private SilverSenadoSenadorDiscursoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorDiscursoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorDiscursoDTO.Pronunciamento pronunciamento(String codigo) {
        SenadoSenadorDiscursoDTO.Pronunciamento p = new SenadoSenadorDiscursoDTO.Pronunciamento();
        p.setCodigoPronunciamento(codigo);
        p.setCodigoSessao("20230515T140000");
        p.setDataPronunciamento("2023-05-15");
        p.setCasa("SF");
        p.setTipoSessao("Ordinária");
        p.setNumeroSessao("42");
        p.setTipoPronunciamento("Discurso");
        p.setTextoPronunciamento("Texto do discurso parlamentar...");
        p.setDuracaoAparte("00:05:00");
        p.setUrlVideo("https://www.senado.leg.br/video/disc123");
        p.setUrlAudio("https://www.senado.leg.br/audio/disc123");
        return p;
    }

    @Test
    @DisplayName("INSERT: discurso novo é salvo com todos os campos (codigoPronunciamento → codigoDiscurso)")
    void inserirDiscursoNovo() {
        SenadoSenadorDiscursoDTO.Pronunciamento p = pronunciamento("3001");

        when(repository.existsByCodigoSenadorAndCodigoDiscurso(CODIGO_SENADOR, "3001")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(p), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorDiscurso> captor = ArgumentCaptor.forClass(SilverSenadoSenadorDiscurso.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorDiscurso salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoDiscurso()).isEqualTo("3001");
        assertThat(salvo.getCodigoSessao()).isEqualTo("20230515T140000");
        assertThat(salvo.getDataPronunciamento()).isEqualTo("2023-05-15");
        assertThat(salvo.getCasa()).isEqualTo("SF");
        assertThat(salvo.getTipoPronunciamento()).isEqualTo("Discurso");
        assertThat(salvo.getTextoDiscurso()).isEqualTo("Texto do discurso parlamentar...");
        assertThat(salvo.getUrlVideo()).isEqualTo("https://www.senado.leg.br/video/disc123");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: discurso já existente não é inserido novamente")
    void ignorarDiscursoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoDiscurso(CODIGO_SENADOR, "3001")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(pronunciamento("3001")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: pronunciamento com codigoPronunciamento nulo é ignorado")
    void ignorarDiscursoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(pronunciamento(null)), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(pronunciamento("3001")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos discursos novos geram múltiplos registros")
    void inserirMultiplosDiscursos() {
        List<SenadoSenadorDiscursoDTO.Pronunciamento> lista = List.of(pronunciamento("1"), pronunciamento("2"),
                pronunciamento("3"));

        when(repository.existsByCodigoSenadorAndCodigoDiscurso(eq(CODIGO_SENADOR), any())).thenReturn(false);
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
