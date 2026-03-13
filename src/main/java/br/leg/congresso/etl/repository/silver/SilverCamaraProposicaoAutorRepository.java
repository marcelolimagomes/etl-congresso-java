package br.leg.congresso.etl.repository.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;

@Repository
public interface SilverCamaraProposicaoAutorRepository extends JpaRepository<SilverCamaraProposicaoAutor, UUID> {

    boolean existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(
            String uriProposicao, String nomeAutor, Integer ordemAssinatura);

    List<SilverCamaraProposicaoAutor> findByCamaraProposicaoIdOrderByOrdemAssinaturaAsc(UUID camaraProposicaoId);

    @Query("SELECT a FROM SilverCamaraProposicaoAutor a LEFT JOIN FETCH a.camaraProposicao p WHERE a.idDeputadoAutor = :idDeputado")
    List<SilverCamaraProposicaoAutor> findByDeputadoAutorWithProposicao(@Param("idDeputado") String idDeputado);
}
