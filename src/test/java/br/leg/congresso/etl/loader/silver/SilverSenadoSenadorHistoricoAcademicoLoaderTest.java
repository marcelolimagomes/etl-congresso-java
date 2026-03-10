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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorHistoricoAcademico;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorHistoricoAcademicoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorHistoricoAcademicoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorHistoricoAcademicoLoader — insert-if-not-exists por (codigo_senador, codigo_curso)")
class SilverSenadoSenadorHistoricoAcademicoLoaderTest {

    @Mock
    private SilverSenadoSenadorHistoricoAcademicoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorHistoricoAcademicoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorHistoricoAcademicoDTO.Curso curso(String codigo) {
        SenadoSenadorHistoricoAcademicoDTO.Curso c = new SenadoSenadorHistoricoAcademicoDTO.Curso();
        c.setCodigoCurso(codigo);
        c.setNomeCurso("Direito");
        c.setInstituicao("USP");
        c.setDescricaoInstituicao("Universidade de São Paulo");
        c.setGrauInstrucao("Superior Completo");
        c.setDataInicioCurso("1985-02-01");
        c.setDataTerminoCurso("1990-12-15");
        c.setConcluido("S");
        return c;
    }

    @Test
    @DisplayName("INSERT: curso novo é salvo com todos os campos (incluindo mapeamento de grauInstrucao)")
    void inserirCursoNovo() {
        SenadoSenadorHistoricoAcademicoDTO.Curso c = curso("10");

        when(repository.existsByCodigoSenadorAndCodigoCurso(CODIGO_SENADOR, "10")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(c), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorHistoricoAcademico> captor = ArgumentCaptor
                .forClass(SilverSenadoSenadorHistoricoAcademico.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorHistoricoAcademico salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoCurso()).isEqualTo("10");
        assertThat(salvo.getNomeCurso()).isEqualTo("Direito");
        assertThat(salvo.getInstituicao()).isEqualTo("USP");
        assertThat(salvo.getDescricaoInstituicao()).isEqualTo("Universidade de São Paulo");
        assertThat(salvo.getNivelFormacao()).isEqualTo("Superior Completo");
        assertThat(salvo.getDataInicioFormacao()).isEqualTo("1985-02-01");
        assertThat(salvo.getDataTerminoFormacao()).isEqualTo("1990-12-15");
        assertThat(salvo.getConcluido()).isEqualTo("S");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: curso já existente não é inserido novamente")
    void ignorarCursoExistente() {
        when(repository.existsByCodigoSenadorAndCodigoCurso(CODIGO_SENADOR, "10")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(curso("10")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: curso com codigoCurso nulo é ignorado")
    void ignorarCursoComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(curso(null)), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(curso("10")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos cursos novos geram múltiplos registros")
    void inserirMultiplosCursos() {
        List<SenadoSenadorHistoricoAcademicoDTO.Curso> lista = List.of(curso("1"), curso("2"));

        when(repository.existsByCodigoSenadorAndCodigoCurso(eq(CODIGO_SENADOR), any())).thenReturn(false);
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
