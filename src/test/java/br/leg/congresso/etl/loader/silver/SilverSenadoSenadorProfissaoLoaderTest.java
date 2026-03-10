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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorProfissao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorProfissaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorProfissaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorProfissaoLoader — insert-if-not-exists por (codigo_senador, codigo_profissao)")
class SilverSenadoSenadorProfissaoLoaderTest {

    @Mock
    private SilverSenadoSenadorProfissaoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorProfissaoLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorProfissaoDTO.Profissao profissao(String codigo, String descricao, String dataRegistro) {
        SenadoSenadorProfissaoDTO.Profissao p = new SenadoSenadorProfissaoDTO.Profissao();
        p.setCodigoProfissao(codigo);
        p.setDescricaoProfissao(descricao);
        p.setDataRegistro(dataRegistro);
        return p;
    }

    @Test
    @DisplayName("INSERT: profissão nova é salva com todos os campos")
    void inserirProfissaoNova() {
        SenadoSenadorProfissaoDTO.Profissao p = profissao("42", "Advogado", "2010-01-15");

        when(repository.existsByCodigoSenadorAndCodigoProfissao(CODIGO_SENADOR, "42")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(p), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorProfissao> captor = ArgumentCaptor
                .forClass(SilverSenadoSenadorProfissao.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorProfissao salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoProfissao()).isEqualTo("42");
        assertThat(salvo.getDescricaoProfissao()).isEqualTo("Advogado");
        assertThat(salvo.getDataRegistro()).isEqualTo("2010-01-15");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: profissão já existente não é inserida novamente")
    void ignorarProfissaoExistente() {
        SenadoSenadorProfissaoDTO.Profissao p = profissao("42", "Advogado", null);

        when(repository.existsByCodigoSenadorAndCodigoProfissao(CODIGO_SENADOR, "42")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: profissão com codigoProfissao nulo é ignorada")
    void ignorarProfissaoComCodigoNulo() {
        SenadoSenadorProfissaoDTO.Profissao p = profissao(null, "Advogado", null);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero sem chamar o repositório")
    void ignorarCodigoSenadorNulo() {
        SenadoSenadorProfissaoDTO.Profissao p = profissao("42", "Advogado", null);

        int resultado = loader.carregar(null, List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas profissões novas geram múltiplos registros")
    void inserirMultiplasProfissoes() {
        List<SenadoSenadorProfissaoDTO.Profissao> lista = List.of(
                profissao("1", "Médico", null),
                profissao("2", "Engenheiro", null));

        when(repository.existsByCodigoSenadorAndCodigoProfissao(eq(CODIGO_SENADOR), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, lista, jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero sem chamar o repositório")
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
