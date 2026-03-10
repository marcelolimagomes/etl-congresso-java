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

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorLicenca;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorLicencaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorLicencaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorLicencaLoader — insert-if-not-exists por (codigo_senador, codigo_licenca)")
class SilverSenadoSenadorLicencaLoaderTest {

    @Mock
    private SilverSenadoSenadorLicencaRepository repository;

    @InjectMocks
    private SilverSenadoSenadorLicencaLoader loader;

    private final UUID jobId = UUID.randomUUID();
    private final String CODIGO_SENADOR = "5988";

    private SenadoSenadorLicencaDTO.Licenca licenca(String codigo) {
        SenadoSenadorLicencaDTO.Licenca l = new SenadoSenadorLicencaDTO.Licenca();
        l.setCodigoLicenca(codigo);
        l.setDataInicio("2022-03-01");
        l.setDataFim("2022-03-15");
        l.setSiglaMotivoLicenca("TSE");
        l.setDescricaoMotivoLicenca("Participação no TSE");
        return l;
    }

    @Test
    @DisplayName("INSERT: licença nova é salva com todos os campos (incluindo mapeamento de motivo)")
    void inserirLicencaNova() {
        SenadoSenadorLicencaDTO.Licenca l = licenca("99");

        when(repository.existsByCodigoSenadorAndCodigoLicenca(CODIGO_SENADOR, "99")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(l), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoSenadorLicenca> captor = ArgumentCaptor.forClass(SilverSenadoSenadorLicenca.class);
        verify(repository).save(captor.capture());

        SilverSenadoSenadorLicenca salvo = captor.getValue();
        assertThat(salvo.getCodigoSenador()).isEqualTo(CODIGO_SENADOR);
        assertThat(salvo.getCodigoLicenca()).isEqualTo("99");
        assertThat(salvo.getDataInicio()).isEqualTo("2022-03-01");
        assertThat(salvo.getDataFim()).isEqualTo("2022-03-15");
        assertThat(salvo.getMotivo()).isEqualTo("TSE");
        assertThat(salvo.getDescricaoMotivo()).isEqualTo("Participação no TSE");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("SKIP: licença já existente não é inserida novamente")
    void ignorarLicencaExistente() {
        when(repository.existsByCodigoSenadorAndCodigoLicenca(CODIGO_SENADOR, "99")).thenReturn(true);

        int resultado = loader.carregar(CODIGO_SENADOR, List.of(licenca("99")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: licença com codigoLicenca nulo é ignorada")
    void ignorarLicencaComCodigoNulo() {
        int resultado = loader.carregar(CODIGO_SENADOR, List.of(licenca(null)), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("SKIP: codigoSenador nulo retorna zero")
    void ignorarCodigoSenadorNulo() {
        int resultado = loader.carregar(null, List.of(licenca("99")), jobId);

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("BATCH: múltiplas licenças novas geram múltiplos registros")
    void inserirMultiplasLicencas() {
        List<SenadoSenadorLicencaDTO.Licenca> lista = List.of(licenca("1"), licenca("2"));

        when(repository.existsByCodigoSenadorAndCodigoLicenca(eq(CODIGO_SENADOR), any())).thenReturn(false);
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
