package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraGrupoMembro;

public interface SilverCamaraGrupoMembroRepository
        extends JpaRepository<SilverCamaraGrupoMembro, UUID> {

    boolean existsByIdDeputadoAndIdGrupoAndDataInicio(String idDeputado, String idGrupo, String dataInicio);
}
