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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAutoriaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoAutoriaLoader — upsert por (materiaId, nomeAutor, codigoTipoAutor)")
class SilverSenadoAutoriaLoaderTest {

    @Mock
    private SilverSenadoAutoriaRepository repository;

    @InjectMocks
    private SilverSenadoAutoriaLoader loader;

    private SilverSenadoMateria materia;

    @BeforeEach
    void setup() {
        materia = SilverSenadoMateria.builder()
                .id(UUID.randomUUID())
                .codigo("131506")
                .sigla("PL")
                .numero("100")
                .ano(2024)
                .build();
    }

    private SenadoAutoriaDTO.Autor autorParlamentar(String nome, String codigoTipo, String codigoParlamentar,
            String partido) {
        SenadoAutoriaDTO.Autor autor = new SenadoAutoriaDTO.Autor();
        autor.setNomeAutor(nome);
        autor.setSexoAutor("M");

        SenadoAutoriaDTO.TipoAutor tipo = new SenadoAutoriaDTO.TipoAutor();
        tipo.setCodigoTipoAutor(codigoTipo);
        tipo.setDescricaoTipoAutor("Senador");
        autor.setTipoAutor(tipo);

        SenadoAutoriaDTO.IdentificacaoParlamentar parl = new SenadoAutoriaDTO.IdentificacaoParlamentar();
        parl.setCodigoParlamentar(codigoParlamentar);
        parl.setNomeParlamentar(nome);
        parl.setSiglaPartidoParlamentar(partido);
        parl.setUfParlamentar("SP");
        autor.setIdentificacaoParlamentar(parl);

        return autor;
    }

    private SenadoAutoriaDTO.Autor autorEntidade(String nome, String codigoTipo) {
        SenadoAutoriaDTO.Autor autor = new SenadoAutoriaDTO.Autor();
        autor.setNomeAutor(nome);

        SenadoAutoriaDTO.TipoAutor tipo = new SenadoAutoriaDTO.TipoAutor();
        tipo.setCodigoTipoAutor(codigoTipo);
        tipo.setDescricaoTipoAutor("Órgão");
        autor.setTipoAutor(tipo);

        return autor;
    }

    @Test
    @DisplayName("deve inserir autores novos (sem duplicata)")
    void carregar_insereLote() {
        SenadoAutoriaDTO.Autor a1 = autorParlamentar("João da Silva", "1", "5678", "PT");
        SenadoAutoriaDTO.Autor a2 = autorEntidade("Câmara dos Deputados", "99");

        when(repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
                materia.getId(), "João da Silva", "1")).thenReturn(false);
        when(repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
                materia.getId(), "Câmara dos Deputados", "99")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(materia, List.of(a1, a2));

        assertThat(resultado).isEqualTo(2);
        ArgumentCaptor<SilverSenadoAutoria> captor = ArgumentCaptor.forClass(SilverSenadoAutoria.class);
        verify(repository, times(2)).save(captor.capture());

        List<SilverSenadoAutoria> salvas = captor.getAllValues();
        assertThat(salvas).extracting(SilverSenadoAutoria::getNomeAutor)
                .containsExactlyInAnyOrder("João da Silva", "Câmara dos Deputados");
    }

    @Test
    @DisplayName("deve ignorar autor já existente (idempotência)")
    void carregar_ignoraDuplicado() {
        SenadoAutoriaDTO.Autor a1 = autorParlamentar("João da Silva", "1", "5678", "PT");

        when(repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
                materia.getId(), "João da Silva", "1")).thenReturn(true);

        int resultado = loader.carregar(materia, List.of(a1));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar 0 para lista vazia")
    void carregar_listaVazia_retornaZero() {
        int resultado = loader.carregar(materia, Collections.emptyList());
        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar 0 quando materia é null")
    void carregar_materiaNula_retornaZero() {
        int resultado = loader.carregar(null, List.of(autorEntidade("Senado", "1")));
        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve ignorar autor sem nome")
    void carregar_autorSemNome_ignorado() {
        SenadoAutoriaDTO.Autor semNome = new SenadoAutoriaDTO.Autor();
        semNome.setTipoAutor(new SenadoAutoriaDTO.TipoAutor());
        semNome.getTipoAutor().setCodigoTipoAutor("1");

        int resultado = loader.carregar(materia, List.of(semNome));
        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve usar 'DESCONHECIDO' como tipo quando TipoAutor for null")
    void carregar_semTipoAutor_usaTipoDesconhecido() {
        SenadoAutoriaDTO.Autor semTipo = new SenadoAutoriaDTO.Autor();
        semTipo.setNomeAutor("Autor Anônimo");

        when(repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
                materia.getId(), "Autor Anônimo", "DESCONHECIDO")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(materia, List.of(semTipo));

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverSenadoAutoria> captor = ArgumentCaptor.forClass(SilverSenadoAutoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCodigoTipoAutor()).isEqualTo("DESCONHECIDO");
    }

    @Test
    @DisplayName("deve mapear campos do parlamentar corretamente")
    void carregar_mapeaCamposParlamentarCorretamente() {
        SenadoAutoriaDTO.Autor autor = autorParlamentar("Maria Souza", "1", "9999", "PSDB");

        when(repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(any(), any(), any()))
                .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(materia, List.of(autor));

        ArgumentCaptor<SilverSenadoAutoria> captor = ArgumentCaptor.forClass(SilverSenadoAutoria.class);
        verify(repository).save(captor.capture());

        SilverSenadoAutoria salva = captor.getValue();
        assertThat(salva.getCodigoParlamentar()).isEqualTo("9999");
        assertThat(salva.getNomeParlamentar()).isEqualTo("Maria Souza");
        assertThat(salva.getSiglaPartido()).isEqualTo("PSDB");
        assertThat(salva.getUfParlamentar()).isEqualTo("SP");
        assertThat(salva.getSexoAutor()).isEqualTo("M");
    }
}
