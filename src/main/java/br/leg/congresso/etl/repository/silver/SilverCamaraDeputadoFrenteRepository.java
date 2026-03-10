package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoFrente;

public interface SilverCamaraDeputadoFrenteRepository
        extends JpaRepository<SilverCamaraDeputadoFrente, UUID> {

    boolean existsByIdDeputadoAndIdFrente(String idDeputado, String idFrente);
}
