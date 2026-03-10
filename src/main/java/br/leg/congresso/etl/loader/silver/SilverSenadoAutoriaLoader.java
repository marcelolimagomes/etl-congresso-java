package br.leg.congresso.etl.loader.silver;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoAutoria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAutoriaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoAutoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega autores de matérias do Senado na camada Silver
 * (silver.senado_autoria).
 *
 * Princípio Silver: passthrough fiel ao payload de
 * GET /dadosabertos/materia/autoria/{codigo}.json — sem normalização.
 *
 * Deduplicação: chave composta (senado_materia_id, nome_autor,
 * codigo_tipo_autor).
 * Autores já existentes são ignorados (imutáveis após registro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoAutoriaLoader {

    private static final String TIPO_DESCONHECIDO = "DESCONHECIDO";

    private final SilverSenadoAutoriaRepository repository;

    /**
     * Persiste os autores de uma matéria Silver, ignorando os já existentes.
     *
     * @param materia entidade Silver da matéria
     * @param autores lista de autores retornados pela API do Senado
     * @return número de autores efetivamente inseridos
     */
    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoAutoriaDTO.Autor> autores) {
        if (materia == null || autores == null || autores.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoAutoriaDTO.Autor dto : autores) {
            String nomeAutor = dto.getNomeAutor();
            String codigoTipo = resolveCodigoTipo(dto);

            if (nomeAutor == null || nomeAutor.isBlank()) {
                continue;
            }

            boolean jaExiste = repository.existsBySenadoMateriaIdAndNomeAutorAndCodigoTipoAutor(
                    materia.getId(), nomeAutor, codigoTipo);

            if (!jaExiste) {
                repository.save(dtoToEntity(materia, dto, codigoTipo));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado autoria: {} novos autores para codigo={}", inseridos, materia.getCodigo());
        }

        return inseridos;
    }

    private String resolveCodigoTipo(SenadoAutoriaDTO.Autor dto) {
        // API atual retorna SiglaTipoAutor como campo plano no Autor
        if (dto.getSiglaTipoAutor() != null && !dto.getSiglaTipoAutor().isBlank()) {
            return dto.getSiglaTipoAutor();
        }
        // Fallback para o objeto TipoAutor (API legada)
        if (dto.getTipoAutor() != null && dto.getTipoAutor().getCodigoTipoAutor() != null) {
            return dto.getTipoAutor().getCodigoTipoAutor();
        }
        return TIPO_DESCONHECIDO;
    }

    private SilverSenadoAutoria dtoToEntity(SilverSenadoMateria materia,
            SenadoAutoriaDTO.Autor dto,
            String codigoTipo) {
        String siglaPartido = null;
        String codigoParlamentar = null;
        String nomeParlamentar = null;
        String ufParlamentar = null;

        if (dto.getIdentificacaoParlamentar() != null) {
            var parl = dto.getIdentificacaoParlamentar();
            codigoParlamentar = parl.getCodigoParlamentar();
            nomeParlamentar = parl.getNomeParlamentar();
            siglaPartido = parl.getSiglaPartidoParlamentar();
            ufParlamentar = parl.getUfParlamentar();
        } else if (dto.getPartidodoParlamentar() != null) {
            siglaPartido = dto.getPartidodoParlamentar().getSiglaPartidoParlamentar();
        }

        String descricaoTipo = dto.getDescricaoTipoAutor() != null
                ? dto.getDescricaoTipoAutor()
                : (dto.getTipoAutor() != null ? dto.getTipoAutor().getDescricaoTipoAutor() : null);

        return SilverSenadoAutoria.builder()
                .senadoMateria(materia)
                .nomeAutor(dto.getNomeAutor())
                .sexoAutor(dto.getSexoAutor())
                .codigoTipoAutor(codigoTipo)
                .descricaoTipoAutor(descricaoTipo)
                .codigoParlamentar(codigoParlamentar)
                .nomeParlamentar(nomeParlamentar)
                .siglaPartido(siglaPartido)
                .ufParlamentar(ufParlamentar)
                .build();
    }
}
