package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorListaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoSenadorLoader — insert-if-not-exists + preserve det_* on update")
class SilverSenadoSenadorLoaderTest {

    @Mock
    private SilverSenadoSenadorRepository repository;

    @InjectMocks
    private SilverSenadoSenadorLoader loader;

    private final UUID jobId = UUID.randomUUID();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SenadoSenadorListaDTO.Parlamentar parlamentar(
            String codigo, String nome, String sexo, String uf, String participacao) {

        SenadoSenadorListaDTO.IdentificacaoParlamentar idp = new SenadoSenadorListaDTO.IdentificacaoParlamentar();
        idp.setCodigoParlamentar(codigo);
        idp.setNomeParlamentar(nome);
        idp.setSexoParlamentar(sexo);
        idp.setUfParlamentar(uf);
        idp.setSiglaPartidoParlamentar("PT");

        SenadoSenadorListaDTO.Mandato mandato = new SenadoSenadorListaDTO.Mandato();
        mandato.setDescricaoParticipacao(participacao);
        mandato.setCodigoLegislatura("57");
        mandato.setDataDesignacao("2019-02-01");

        SenadoSenadorListaDTO.Parlamentar p = new SenadoSenadorListaDTO.Parlamentar();
        p.setIdentificacaoParlamentar(idp);
        p.setMandato(mandato);
        return p;
    }

    private SenadoSenadorDetalheDTO detalheDto(String codigo) {
        SenadoSenadorDetalheDTO.IdentificacaoParlamentar idp = new SenadoSenadorDetalheDTO.IdentificacaoParlamentar();
        idp.setNomeCompletoParlamentar("Nome Civil Completo");
        idp.setEmailParlamentar("senador@senado.leg.br");
        idp.setUrlFotoParlamentar("https://www.senado.leg.br/foto.jpg");
        idp.setUrlPaginaParlamentar("https://www.senado.leg.br/senadores/senador.aspx");
        idp.setUrlPaginaParticular("https://senador.com.br");

        SenadoSenadorDetalheDTO.DadosBasicosParlamentar dados = new SenadoSenadorDetalheDTO.DadosBasicosParlamentar();
        dados.setDataNascimento("1965-04-20");
        dados.setNaturalidade("São Paulo");
        dados.setEstadoCivil("Casado");
        dados.setEscolaridade("Superior");

        SenadoSenadorDetalheDTO.OutrasInformacoes outras = new SenadoSenadorDetalheDTO.OutrasInformacoes();
        outras.setFacebook("https://fb.com/senador");
        outras.setTwitter("https://twitter.com/senador");

        SenadoSenadorDetalheDTO.Parlamentar p = new SenadoSenadorDetalheDTO.Parlamentar();
        p.setIdentificacaoParlamentar(idp);
        p.setDadosBasicosParlamentar(dados);
        p.setOutrasInformacoes(outras);

        SenadoSenadorDetalheDTO.DetalheParlamentar dp = new SenadoSenadorDetalheDTO.DetalheParlamentar();
        dp.setParlamentar(p);

        SenadoSenadorDetalheDTO dto = new SenadoSenadorDetalheDTO();
        dto.setDetalheParlamentar(dp);
        return dto;
    }

    // ── Testes de carregar() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("carregar()")
    class Carregar {

        @Test
        @DisplayName("INSERT: senador novo é salvo com todos os campos")
        void inserirSenadorNovo() {
            SenadoSenadorListaDTO.Parlamentar p = parlamentar("5988", "Rodrigo Pacheco", "Masculino", "MG", "Titular");
            when(repository.existsByCodigoSenador("5988")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int resultado = loader.carregar(List.of(p), jobId);

            assertThat(resultado).isEqualTo(1);
            ArgumentCaptor<SilverSenadoSenador> captor = ArgumentCaptor.forClass(SilverSenadoSenador.class);
            verify(repository).save(captor.capture());

            SilverSenadoSenador salvo = captor.getValue();
            assertThat(salvo.getCodigoSenador()).isEqualTo("5988");
            assertThat(salvo.getNomeParlamentar()).isEqualTo("Rodrigo Pacheco");
            assertThat(salvo.getSexo()).isEqualTo("M"); // Masculino → M
            assertThat(salvo.getUfParlamentar()).isEqualTo("MG");
            assertThat(salvo.getParticipacao()).isEqualTo("T"); // Titular → T
            assertThat(salvo.getSiglaPartidoParlamentar()).isEqualTo("PT");
            assertThat(salvo.getCodigoLegislatura()).isEqualTo("57");
            assertThat(salvo.getDataDesignacao()).isEqualTo("2019-02-01");
            assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
            assertThat(salvo.getOrigemCarga()).isEqualTo("API");
        }

        @Test
        @DisplayName("MAPEAMENTO: Feminino → F, Suplente → S")
        void mapearSexoFemininoESuplente() {
            SenadoSenadorListaDTO.Parlamentar p = parlamentar("1234", "Senadora XYZ", "Feminino", "SP", "Suplente");
            when(repository.existsByCodigoSenador("1234")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            loader.carregar(List.of(p), jobId);

            ArgumentCaptor<SilverSenadoSenador> captor = ArgumentCaptor.forClass(SilverSenadoSenador.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getSexo()).isEqualTo("F");
            assertThat(captor.getValue().getParticipacao()).isEqualTo("S");
        }

        @Test
        @DisplayName("SKIP: senador já existente não é inserido novamente")
        void ignorarSenadorExistente() {
            SenadoSenadorListaDTO.Parlamentar p = parlamentar("5988", "Rodrigo Pacheco", "Masculino", "MG", "Titular");
            when(repository.existsByCodigoSenador("5988")).thenReturn(true);

            int resultado = loader.carregar(List.of(p), jobId);

            assertThat(resultado).isEqualTo(0);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("SKIP: DTO com codigoParlamentar nulo é ignorado")
        void ignorarDtoComCodigoNulo() {
            SenadoSenadorListaDTO.Parlamentar p = parlamentar(null, "Nome", "M", "SP", "Titular");

            int resultado = loader.carregar(List.of(p), jobId);

            assertThat(resultado).isEqualTo(0);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("SKIP: DTO com codigoParlamentar vazio é ignorado")
        void ignorarDtoComCodigoVazio() {
            SenadoSenadorListaDTO.Parlamentar p = parlamentar("  ", "Nome", "M", "SP", "Titular");

            int resultado = loader.carregar(List.of(p), jobId);

            assertThat(resultado).isEqualTo(0);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("BATCH: múltiplos senadores novos são inseridos")
        void inserirMultiplosSenadores() {
            SenadoSenadorListaDTO.Parlamentar p1 = parlamentar("5988", "Rodrigo Pacheco", "Masculino", "MG", "Titular");
            SenadoSenadorListaDTO.Parlamentar p2 = parlamentar("6000", "Outra Senadora", "Feminino", "SP", "Titular");
            when(repository.existsByCodigoSenador(any())).thenReturn(false);
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

    // ── Testes de carregarDetalhe() ────────────────────────────────────────────

    @Nested
    @DisplayName("carregarDetalhe()")
    class CarregarDetalhe {

        @Test
        @DisplayName("UPDATE: campos det_* são atualizados quando o senador existe")
        void atualizarDetQuandoSenadorExiste() {
            SilverSenadoSenador existing = SilverSenadoSenador.builder()
                    .codigoSenador("5988")
                    .nomeParlamentar("Rodrigo Pacheco")
                    .build();
            when(repository.findByCodigoSenador("5988")).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean resultado = loader.carregarDetalhe("5988", detalheDto("5988"), jobId);

            assertThat(resultado).isTrue();
            ArgumentCaptor<SilverSenadoSenador> captor = ArgumentCaptor.forClass(SilverSenadoSenador.class);
            verify(repository).save(captor.capture());

            SilverSenadoSenador atualizado = captor.getValue();
            assertThat(atualizado.getDetNomeCompleto()).isEqualTo("Nome Civil Completo");
            assertThat(atualizado.getDetContatoEmail()).isEqualTo("senador@senado.leg.br");
            assertThat(atualizado.getDetDataNascimento()).isEqualTo("1965-04-20");
            assertThat(atualizado.getDetLocalNascimento()).isEqualTo("São Paulo");
            assertThat(atualizado.getDetEstadoCivil()).isEqualTo("Casado");
            assertThat(atualizado.getDetEscolaridade()).isEqualTo("Superior");
            assertThat(atualizado.getDetFacebook()).isEqualTo("https://fb.com/senador");
            assertThat(atualizado.getDetTwitter()).isEqualTo("https://twitter.com/senador");
            assertThat(atualizado.getEtlJobId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("FALSE: retorna false quando o senador não existe")
        void falseQuandoSenadorNaoExiste() {
            when(repository.findByCodigoSenador("9999")).thenReturn(Optional.empty());

            boolean resultado = loader.carregarDetalhe("9999", detalheDto("9999"), jobId);

            assertThat(resultado).isFalse();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("FALSE: retorna false para código nulo")
        void falseParaCodigoNulo() {
            boolean resultado = loader.carregarDetalhe(null, detalheDto("5988"), jobId);

            assertThat(resultado).isFalse();
            verify(repository, never()).save(any());
            verify(repository, never()).findByCodigoSenador(any());
        }
    }
}
