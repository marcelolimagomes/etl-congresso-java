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

import br.leg.congresso.etl.domain.silver.SilverSenadoPartido;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPartidoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoPartidoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoPartidoLoader — insert-if-not-exists por codigo_partido")
class SilverSenadoPartidoLoaderTest {

    @Mock
    private SilverSenadoPartidoRepository repository;

    @InjectMocks
    private SilverSenadoPartidoLoader loader;

    private final UUID jobId = UUID.randomUUID();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SenadoPartidoDTO.Partido partido(
            String codigo, String sigla, String nome,
            String dataAtivacao, String dataDesativacao) {

        SenadoPartidoDTO.Partido p = new SenadoPartidoDTO.Partido();
        p.setCodigoPartido(codigo);
        p.setSiglaPartido(sigla);
        p.setNomePartido(nome);
        p.setDataAtivacao(dataAtivacao);
        p.setDataDesativacao(dataDesativacao);
        return p;
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: partido novo é salvo com todos os campos")
    void inserirPartidoNovo() {
        SenadoPartidoDTO.Partido p = partido("36723", "PT", "Partido dos Trabalhadores", "1980-10-10", null);
        when(repository.existsByCodigoPartido("36723")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoPartido> captor = ArgumentCaptor.forClass(SilverSenadoPartido.class);
        verify(repository).save(captor.capture());

        SilverSenadoPartido salvo = captor.getValue();
        assertThat(salvo.getCodigoPartido()).isEqualTo("36723");
        assertThat(salvo.getSiglaPartido()).isEqualTo("PT");
        assertThat(salvo.getNomePartido()).isEqualTo("Partido dos Trabalhadores");
        assertThat(salvo.getDataAtivacao()).isEqualTo("1980-10-10");
        assertThat(salvo.getDataDesativacao()).isNull();
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("API");
    }

    @Test
    @DisplayName("FALLBACK: dataAtivacao usa DataCriacao quando DataAtivacao é nulo")
    void fallbackDataAtivacao() {
        SenadoPartidoDTO.Partido p = new SenadoPartidoDTO.Partido();
        p.setCodigoPartido("100");
        p.setSiglaPartido("XX");
        p.setNomePartido("Partido XX");
        p.setDataAtivacao(null); // DataAtivacao ausente
        p.setDataCriacao("1999-01-01"); // Deve usar DataCriacao como fallback
        p.setDataDesativacao(null);
        p.setDataExtincao("2005-12-31"); // Deve usar DataExtincao como fallback

        when(repository.existsByCodigoPartido("100")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(p), jobId);

        ArgumentCaptor<SilverSenadoPartido> captor = ArgumentCaptor.forClass(SilverSenadoPartido.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getDataAtivacao()).isEqualTo("1999-01-01");
        assertThat(captor.getValue().getDataDesativacao()).isEqualTo("2005-12-31");
    }

    @Test
    @DisplayName("SKIP: partido já existente não é inserido novamente")
    void ignorarPartidoExistente() {
        SenadoPartidoDTO.Partido p = partido("36723", "PT", "Partido dos Trabalhadores", "1980-10-10", null);
        when(repository.existsByCodigoPartido("36723")).thenReturn(true);

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: partido com codigoPartido nulo é ignorado")
    void ignorarPartidoComCodigoNulo() {
        SenadoPartidoDTO.Partido p = partido(null, "XX", "Partido XX", null, null);

        int resultado = loader.carregar(List.of(p), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplos partidos novos são inseridos")
    void inserirMultiplosPartidos() {
        SenadoPartidoDTO.Partido p1 = partido("36723", "PT", "Partido dos Trabalhadores", "1980-10-10", null);
        SenadoPartidoDTO.Partido p2 = partido("36844", "PSDB", "Partido da Social Democracia Brasileira", "1988-06-25",
                null);
        when(repository.existsByCodigoPartido(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(List.of(p1, p2), jobId);

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
