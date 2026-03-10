package br.leg.congresso.etl.loader.silver;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Componente responsável por carregar uma matéria Silver do Senado e aplicar
 * campos de enriquecimento dentro de uma transação gerenciada.
 *
 * <p>
 * Separado do {@link SilverEnrichmentService} para garantir que a entidade
 * seja <em>gerenciada</em> (managed) pelo EntityManager quando o enriquecimento
 * for aplicado. Desta forma, o Hibernate detecta as mudanças automaticamente
 * via dirty-checking e as propaga ao banco ao fazer commit — sem depender de
 * {@code save()} em entidades desconectadas.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class SilverSenadoEnriquecedor {

    private final SilverSenadoMateriaRepository repository;

    /**
     * Carrega a matéria pelo código em uma transação read-write, aplica o
     * enriquecedor e permite que o Hibernate propague o UPDATE via dirty-checking.
     *
     * @param codigo   código da matéria no Senado
     * @param enricher função que modifica a entidade gerenciada
     * @return {@code true} se a matéria foi encontrada e enriquecida
     */
    @Transactional
    public boolean enriquecer(String codigo, Consumer<SilverSenadoMateria> enricher) {
        return repository.findByCodigo(codigo)
                .map(silver -> {
                    enricher.accept(silver);
                    return true;
                })
                .orElse(false);
    }
}
