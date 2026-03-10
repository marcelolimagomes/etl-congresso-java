package br.leg.congresso.etl.repository.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;

@Repository
public interface SilverCamaraProposicaoTemaRepository extends JpaRepository<SilverCamaraProposicaoTema, UUID> {

    boolean existsByUriProposicaoAndCodTema(String uriProposicao, Integer codTema);

    @Query("SELECT COUNT(t) FROM SilverCamaraProposicaoTema t WHERE t.ano = :ano")
    long countByAno(@Param("ano") Integer ano);

    List<SilverCamaraProposicaoTema> findByCamaraProposicaoId(UUID camaraProposicaoId);
}
