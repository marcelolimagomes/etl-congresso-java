package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverSenadoVotacao;

@Repository
public interface SilverSenadoVotacaoRepository extends JpaRepository<SilverSenadoVotacao, UUID> {

    boolean existsBySenadoMateriaIdAndCodigoSessaoVotacaoAndSequencialSessao(
            UUID senadoMateriaId,
            String codigoSessaoVotacao,
            String sequencialSessao);

    java.util.List<SilverSenadoVotacao> findBySenadoMateriaId(UUID senadoMateriaId);
}
