package br.leg.congresso.etl.repository.silver;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoOrientacao;

@Repository
public interface SilverCamaraVotacaoOrientacaoRepository extends JpaRepository<SilverCamaraVotacaoOrientacao, UUID> {

    boolean existsByIdVotacaoAndSiglaBancada(String idVotacao, String siglaBancada);

    List<SilverCamaraVotacaoOrientacao> findAllByIdVotacaoIn(Collection<String> idVotacao);
}
