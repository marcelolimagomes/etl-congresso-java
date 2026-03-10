package br.leg.congresso.etl.repository.silver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;

@Repository
public interface SilverSenadoMateriaRepository extends JpaRepository<SilverSenadoMateria, UUID> {

    Optional<SilverSenadoMateria> findByCodigo(String codigo);

    List<SilverSenadoMateria> findAllByCodigoIn(Collection<String> codigos);

    List<SilverSenadoMateria> findByGoldSincronizadoFalse();

    /** Retorna registros ainda não enriquecidos com dados do endpoint de detalhe */
    List<SilverSenadoMateria> findByDetSiglaCasaIdentificacaoIsNull();

    /** Retorna registros já enriquecidos com dados do endpoint de detalhe */
    List<SilverSenadoMateria> findByDetSiglaCasaIdentificacaoIsNotNull();

    @Query("SELECT COUNT(s) FROM SilverSenadoMateria s WHERE s.ano = :ano")
    long countByAno(@Param("ano") Integer ano);

    long countByDetSiglaCasaIdentificacaoIsNull();

    @Query("SELECT COUNT(s) FROM SilverSenadoMateria s WHERE s.ano = :ano AND s.detSiglaCasaIdentificacao IS NULL")
    long countPendentesEnriquecimentoByAno(@Param("ano") Integer ano);

    @Query("SELECT COUNT(s) FROM SilverSenadoMateria s WHERE s.goldSincronizado = false")
    long countPendentesPromocao();

    @Modifying
    @Query("UPDATE SilverSenadoMateria s SET s.goldSincronizado = true WHERE s.id = :id")
    void marcarGoldSincronizado(@Param("id") UUID id);
}
