package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraMesaDiretora;

public interface SilverCamaraMesaDiretoraRepository
        extends JpaRepository<SilverCamaraMesaDiretora, UUID> {

    boolean existsByIdDeputadoAndIdLegislaturaAndTituloAndDataInicio(
            String idDeputado, String idLegislatura, String titulo, String dataInicio);
}
