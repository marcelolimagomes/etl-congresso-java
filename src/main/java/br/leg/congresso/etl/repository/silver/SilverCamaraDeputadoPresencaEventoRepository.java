package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoPresencaEvento;

public interface SilverCamaraDeputadoPresencaEventoRepository
        extends JpaRepository<SilverCamaraDeputadoPresencaEvento, UUID> {

    boolean existsByIdDeputadoAndIdEvento(String idDeputado, String idEvento);
}
