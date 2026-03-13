package br.leg.congresso.etl.pagegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParlamentaresSitemapGenerator")
@SuppressWarnings("null")
class ParlamentaresSitemapGeneratorTest {

    @Mock
    SilverCamaraDeputadoRepository deputadoRepository;
    @Mock
    SilverSenadoSenadorRepository senadorRepository;

    @InjectMocks
    ParlamentaresSitemapGenerator sitemapGenerator;

    @Test
    @DisplayName("gera sitemap com URLs de deputados e senadores")
    void geraSitemap(@TempDir Path dir) throws IOException {
        var deputado = SilverCamaraDeputado.builder()
                .camaraId("204554")
                .atualizadoEm(LocalDateTime.of(2026, 3, 10, 12, 0))
                .build();
        var senador = SilverSenadoSenador.builder()
                .codigoSenador("5529")
                .atualizadoEm(LocalDateTime.of(2026, 3, 11, 12, 0))
                .build();

        when(deputadoRepository.count()).thenReturn(1L);
        when(senadorRepository.count()).thenReturn(1L);
        when(deputadoRepository.findAll(PageRequest.of(0, 1000)))
                .thenReturn(new PageImpl<>(List.of(deputado), PageRequest.of(0, 1000), 1));
        when(senadorRepository.findAll(PageRequest.of(0, 1000)))
                .thenReturn(new PageImpl<>(List.of(senador), PageRequest.of(0, 1000), 1));

        sitemapGenerator.generate(dir);

        Path sitemap = dir.resolve("sitemap-parlamentares.xml");
        String content = java.nio.file.Files.readString(sitemap);
        assertThat(sitemap).exists();
        assertThat(content).contains("https://www.translegis.com.br/parlamentares/camara-204554");
        assertThat(content).contains("https://www.translegis.com.br/parlamentares/senado-5529");
        assertThat(content).contains("<lastmod>2026-03-10</lastmod>");
        assertThat(content).contains("<lastmod>2026-03-11</lastmod>");
    }

    @Test
    @DisplayName("não gera sitemap quando não há parlamentares")
    void naoGeraSitemapSemDados(@TempDir Path dir) {
        when(deputadoRepository.count()).thenReturn(0L);
        when(senadorRepository.count()).thenReturn(0L);

        sitemapGenerator.generate(dir);

        assertThat(dir.resolve("sitemap-parlamentares.xml")).doesNotExist();
    }
}