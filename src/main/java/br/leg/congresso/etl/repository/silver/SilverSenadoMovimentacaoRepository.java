package br.leg.congresso.etl.repository.silver;

import br.leg.congresso.etl.domain.silver.SilverSenadoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SilverSenadoMovimentacaoRepository extends JpaRepository<SilverSenadoMovimentacao, UUID> {

    List<SilverSenadoMovimentacao> findBySenadoMateriaId(UUID senadoMateriaId);

    boolean existsBySenadoMateriaIdAndSequenciaMovimentacao(UUID senadoMateriaId, String sequenciaMovimentacao);

    @Query("SELECT COUNT(m) FROM SilverSenadoMovimentacao m WHERE m.senadoMateria.ano = :ano")
    long countByMateriaAno(@Param("ano") Integer ano);
}
