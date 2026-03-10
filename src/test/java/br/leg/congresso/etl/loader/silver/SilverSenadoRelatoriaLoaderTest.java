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

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoRelatoria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRelatoriaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoRelatoriaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverSenadoRelatoriaLoader — upsert por (materiaId, idRelatoria)")
class SilverSenadoRelatoriaLoaderTest {

    @Mock
    private SilverSenadoRelatoriaRepository repository;

    @InjectMocks
    private SilverSenadoRelatoriaLoader loader;

    private SilverSenadoMateria materia;

    @BeforeEach
    void setup() {
        materia = SilverSenadoMateria.builder()
                .id(UUID.randomUUID())
                .codigo("166705")
                .sigla("PDL")
                .numero("361")
                .ano(2024)
                .build();
    }

    private SenadoRelatoriaDTO relatoriaDto(long id, String nomeParlamentar, String siglaColegiado) {
        SenadoRelatoriaDTO dto = new SenadoRelatoriaDTO();
        dto.setId(id);
        dto.setCasaRelator("SF");
        dto.setIdTipoRelator(1L);
        dto.setDescricaoTipoRelator("Relator");
        dto.setDataDesignacao("2024-12-17");
        dto.setNomeParlamentar(nomeParlamentar);
        dto.setSiglaColegiado(siglaColegiado);
        dto.setNomeColegiado("Plenário do Senado");
        dto.setSiglaCasa("SF");
        dto.setCodigoParlamentar(6009L);
        dto.setSiglaPartidoParlamentar("PL");
        dto.setUfParlamentar("SP");
        return dto;
    }

    @Test
    @DisplayName("deve inserir lote de novas relatorias")
    void carregar_insereLote() {
        SenadoRelatoriaDTO r1 = relatoriaDto(1001L, "Senador A", "PLEN");
        SenadoRelatoriaDTO r2 = relatoriaDto(1002L, "Senador B", "CAS");

        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 1001L)).thenReturn(false);
        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 1002L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(materia, List.of(r1, r2));

        assertThat(resultado).isEqualTo(2);
        verify(repository, times(2)).save(any(SilverSenadoRelatoria.class));
    }

    @Test
    @DisplayName("deve ignorar relatoria já existente (idempotência)")
    void carregar_ignoraDuplicado() {
        SenadoRelatoriaDTO r1 = relatoriaDto(1001L, "Senador A", "PLEN");

        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 1001L)).thenReturn(true);

        int resultado = loader.carregar(materia, List.of(r1));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar zero para lista vazia")
    void carregar_listaVazia_retornaZero() {
        int resultado = loader.carregar(materia, Collections.emptyList());

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).existsBySenadoMateriaIdAndIdRelatoria(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar zero quando matéria é nula")
    void carregar_materiaNula_retornaZero() {
        SenadoRelatoriaDTO r1 = relatoriaDto(1001L, "Senador A", "PLEN");

        int resultado = loader.carregar(null, List.of(r1));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve ignorar relatoria com id nulo")
    void carregar_relatoriaComIdNulo_ignorado() {
        SenadoRelatoriaDTO dto = new SenadoRelatoriaDTO();
        dto.setId(null);
        dto.setCasaRelator("SF");

        int resultado = loader.carregar(materia, List.of(dto));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve mapear todos os campos corretamente na entidade")
    void carregar_mapeaCamposCorretamente() {
        SenadoRelatoriaDTO dto = new SenadoRelatoriaDTO();
        dto.setId(9878123L);
        dto.setCasaRelator("SF");
        dto.setIdTipoRelator(1L);
        dto.setDescricaoTipoRelator("Relator");
        dto.setDataDesignacao("2024-12-17");
        dto.setDataDestituicao("2024-12-18");
        dto.setDescricaoTipoEncerramento("Deliberação da matéria");
        dto.setIdProcesso(8774796L);
        dto.setIdentificacaoProcesso("PDL 361/2024");
        dto.setTramitando("N");
        dto.setCodigoParlamentar(6009L);
        dto.setNomeParlamentar("Astronauta Marcos Pontes");
        dto.setNomeCompleto("Marcos Cesar Pontes");
        dto.setSexoParlamentar("M");
        dto.setFormaTratamentoParlamentar("Senador");
        dto.setSiglaPartidoParlamentar("PL");
        dto.setUfParlamentar("SP");
        dto.setCodigoColegiado(1998L);
        dto.setSiglaCasa("SF");
        dto.setSiglaColegiado("PLEN");
        dto.setNomeColegiado("Plenário do Senado Federal");
        dto.setCodigoTipoColegiado(128L);

        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 9878123L)).thenReturn(false);
        ArgumentCaptor<SilverSenadoRelatoria> captor = ArgumentCaptor.forClass(SilverSenadoRelatoria.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(materia, List.of(dto));

        SilverSenadoRelatoria salva = captor.getValue();
        assertThat(salva.getIdRelatoria()).isEqualTo(9878123L);
        assertThat(salva.getCasaRelator()).isEqualTo("SF");
        assertThat(salva.getDescricaoTipoRelator()).isEqualTo("Relator");
        assertThat(salva.getDataDesignacao()).isEqualTo("2024-12-17");
        assertThat(salva.getDataDestituicao()).isEqualTo("2024-12-18");
        assertThat(salva.getIdProcesso()).isEqualTo(8774796L);
        assertThat(salva.getIdentificacaoProcesso()).isEqualTo("PDL 361/2024");
        assertThat(salva.getTramitando()).isEqualTo("N");
        assertThat(salva.getCodigoParlamentar()).isEqualTo(6009L);
        assertThat(salva.getNomeParlamentar()).isEqualTo("Astronauta Marcos Pontes");
        assertThat(salva.getNomeCompleto()).isEqualTo("Marcos Cesar Pontes");
        assertThat(salva.getSexoParlamentar()).isEqualTo("M");
        assertThat(salva.getFormaTratamentoParlamentar()).isEqualTo("Senador");
        assertThat(salva.getSiglaPartidoParlamentar()).isEqualTo("PL");
        assertThat(salva.getUfParlamentar()).isEqualTo("SP");
        assertThat(salva.getCodigoColegiado()).isEqualTo(1998L);
        assertThat(salva.getSiglaCasa()).isEqualTo("SF");
        assertThat(salva.getSiglaColegiado()).isEqualTo("PLEN");
        assertThat(salva.getNomeColegiado()).isEqualTo("Plenário do Senado Federal");
        assertThat(salva.getCodigoTipoColegiado()).isEqualTo(128L);
        assertThat(salva.getSenadoMateria()).isEqualTo(materia);
    }

    @Test
    @DisplayName("deve processar apenas novos registros em lote misto")
    void carregar_loteMisto_insereSomenteNovos() {
        SenadoRelatoriaDTO nova = relatoriaDto(2001L, "Senador Novo", "PLEN");
        SenadoRelatoriaDTO duplicada = relatoriaDto(2002L, "Senador Dup", "CAS");

        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 2001L)).thenReturn(false);
        when(repository.existsBySenadoMateriaIdAndIdRelatoria(materia.getId(), 2002L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(materia, List.of(nova, duplicada));

        assertThat(resultado).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
