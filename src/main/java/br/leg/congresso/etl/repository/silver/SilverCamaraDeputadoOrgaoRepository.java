package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOrgao;

public interface SilverCamaraDeputadoOrgaoRepository
        extends JpaRepository<SilverCamaraDeputadoOrgao, UUID> {

    boolean existsByIdDeputadoAndIdOrgaoAndDataInicio(
            String idDeputado, String idOrgao, String dataInicio);
}
