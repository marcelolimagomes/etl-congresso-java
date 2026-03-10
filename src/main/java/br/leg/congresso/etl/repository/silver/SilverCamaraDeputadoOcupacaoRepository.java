package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOcupacao;

public interface SilverCamaraDeputadoOcupacaoRepository
        extends JpaRepository<SilverCamaraDeputadoOcupacao, UUID> {

    boolean existsByIdDeputadoAndTituloAndAnoInicio(
            String idDeputado, String titulo, String anoInicio);
}
