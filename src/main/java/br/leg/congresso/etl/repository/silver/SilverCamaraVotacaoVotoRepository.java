package br.leg.congresso.etl.repository.silver;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoVoto;

@Repository
public interface SilverCamaraVotacaoVotoRepository extends JpaRepository<SilverCamaraVotacaoVoto, UUID> {

    boolean existsByIdVotacaoAndDeputadoId(String idVotacao, Integer deputadoId);

    List<SilverCamaraVotacaoVoto> findAllByIdVotacaoIn(Collection<String> idVotacao);
}
