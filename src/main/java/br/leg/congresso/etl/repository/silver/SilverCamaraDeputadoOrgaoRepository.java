package br.leg.congresso.etl.repository.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOrgao;

public interface SilverCamaraDeputadoOrgaoRepository
        extends JpaRepository<SilverCamaraDeputadoOrgao, UUID> {

    boolean existsByIdDeputadoAndIdOrgaoAndDataInicio(
            String idDeputado, String idOrgao, String dataInicio);

        List<SilverCamaraDeputadoOrgao> findByIdDeputadoOrderByDataInicioDesc(String idDeputado, Pageable pageable);
}
