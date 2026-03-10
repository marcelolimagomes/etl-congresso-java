package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoDiscurso;

/**
 * Repositório Silver — discursos de deputados da Câmara.
 * Tabela: silver.camara_deputado_discurso
 */
@Repository
public interface SilverCamaraDeputadoDiscursoRepository
        extends JpaRepository<SilverCamaraDeputadoDiscurso, UUID> {

    /**
     * Verifica se já existe discurso com a chave natural composta.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dataHoraInicio   data/hora de início do discurso
     * @param tipoDiscurso     tipo do discurso
     */
    boolean existsByCamaraDeputadoIdAndDataHoraInicioAndTipoDiscurso(
            String camaraDeputadoId, String dataHoraInicio, String tipoDiscurso);
}
