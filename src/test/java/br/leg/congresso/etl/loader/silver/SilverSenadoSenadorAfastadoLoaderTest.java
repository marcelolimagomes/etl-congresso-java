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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAfastado;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAfastadoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorAfastadoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorAfastadoLoader — insert-if-not-exists por (codigo_senador, data_afastamento)")
class SilverSenadoSenadorAfastadoLoaderTest {

    @Mock
    private SilverSenadoSenadorAfastadoRepository repository;

    @InjectMocks
    private SilverSenadoSenadorAfastadoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SenadoAfastadoDTO.Parlamentar parlamentarComAfastamento(
            String codigo, String nome, String uf,
            String causa, String dataAfastamento, String dataTermino) {

        SenadoAfastadoDTO.IdentificacaoParlamentar idp = new SenadoAfastadoDTO.IdentificacaoParlamentar();
        idp.setCodigoParlamentar(codigo);
        idp.setNomeParlamentar(nome);
        idp.setUfParlamentar(uf);

        SenadoAfastadoDTO.Afastamento afastamento = new SenadoAfastadoDTO.Afastamento();
        afastamento.setDescricaoCausaAfastamento(causa);
        afastamento.setDataAfastamento(dataAfastamento);
        afastamento.setDataTerminoAfastamento(dataTermino);

        SenadoAfastadoDTO.Afastamentos afastamentos = new SenadoAfastadoDTO.Afastamentos();
        afastamentos.setAfastamento(List.of(afastamento));

        SenadoAfastadoDTO.Parlamentar p = new SenadoAfastadoDTO.Parlamentar();
        p.setIdentificacaoParlamentar(idp);
        p.setAfastamentos(afastamentos);
        return p;
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: afastamento novo é salvo com todos os campos")
    void inserirAfastamentoNovo() {
        SenadoAfastadoDTO.Parlamentar p = parlamentarComAfastamento(
                "5988", "Rodrigo Pacheco", "MG", "Licença para tratamento de saúde", "2024-03-01", "2024-03-15");

        when(repository.existsByCodigoSenadorAndDataAfastamento("5988", "2024-03-01")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorAfastado> captor = ArgumentCaptor.forClass(SilverSenadoSenadorAfastado.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorAfastado salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo("5988");
        assertThat(salvo.getNomeParlamentar()).isEqualTo("Rodrigo Pacheco");
        assertThat(salvo.getUfMandato()).isEqualTo("MG");
        assertThat(salvo.getMotivoAfastamento()).isEqualTo("Licença para tratamento de saúde");
        assertThat(salvo.getDataAfastamento()).isEqualTo("2024-03-01");
        assertThat(salvo.getDataTerminoAfastamento()).isEqualTo("2024-03-15");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: afastamento já existente não é inserido novamente")
    void ignorarAfastamentoExistente() {
        SenadoAfastadoDTO.Parlamentar p = parlamentarComAfastamento(
                "5988", "Rodrigo Pacheco", "MG", "Licença", "2024-03-01", null);

        when(repository.existsByCodigoSenadorAndDataAfastamento("5988", "2024-03-01")).thenReturn(true);

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: parlamentar com codigoParlamentar nulo é ignorado")
    void ignorarParlamentarComCodigoNulo() {
        SenadoAfastadoDTO.Parlamentar p = parlamentarComAfastamento(
                null, "Nome", "SP", "Licença", "2024-03-01", null);

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: afastamento com dataAfastamento nula é ignorado")
    void ignorarAfastamentoComDataNula() {
        SenadoAfastadoDTO.Parlamentar p = parlamentarComAfastamento(
                "5988", "Rodrigo Pacheco", "MG", "Licença", null, null);

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: parlamentar com múltiplos afastamentos gera múltiplos registros")
    void inserirMultiplosAfastamentosParaUmParlamentar() {
        SenadoAfastadoDTO.IdentificacaoParlamentar idp = new SenadoAfastadoDTO.IdentificacaoParlamentar();
        idp.setCodigoParlamentar("5988");
        idp.setNomeParlamentar("Rodrigo Pacheco");
        idp.setUfParlamentar("MG");

        SenadoAfastadoDTO.Afastamento af1 = new SenadoAfastadoDTO.Afastamento();
        af1.setDataAfastamento("2024-01-01");
        af1.setDescricaoCausaAfastamento("Licença 1");

        SenadoAfastadoDTO.Afastamento af2 = new SenadoAfastadoDTO.Afastamento();
        af2.setDataAfastamento("2024-03-01");
        af2.setDescricaoCausaAfastamento("Licença 2");

        SenadoAfastadoDTO.Afastamentos afastamentos = new SenadoAfastadoDTO.Afastamentos();
        afastamentos.setAfastamento(List.of(af1, af2));

        SenadoAfastadoDTO.Parlamentar p = new SenadoAfastadoDTO.Parlamentar();
        p.setIdentificacaoParlamentar(idp);
        p.setAfastamentos(afastamentos);

        when(repository.existsByCodigoSenadorAndDataAfastamento(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("EMPTY: lista vazia retorna zero sem chamar o repositório")
    void listaVaziaRetornaZero() {
        int resultado = loader.carregar(Collections.emptyList(), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("NULL: lista nula retorna zero sem lançar exceção")
    void listaNulaRetornaZero() {
        int resultado = loader.carregar(null, jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }
}
