package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoMandatoExterno;

/**
 * Repositório Silver — mandatos externos de deputados da Câmara.
 * Tabela: silver.camara_deputado_mandato_externo
 */
@Repository
public interface SilverCamaraDeputadoMandatoExternoRepository
        extends JpaRepository<SilverCamaraDeputadoMandatoExterno, UUID> {

    /**
     * Verifica se já existe mandato externo com a chave natural composta.
     *
     * @param camaraDeputadoId ID do deputado
     * @param cargo            cargo exercido
     * @param siglaUf          UF de exercício do cargo
     * @param anoInicio        ano de início do mandato
     */
    boolean existsByCamaraDeputadoIdAndCargoAndSiglaUfAndAnoInicio(
            String camaraDeputadoId, String cargo, String siglaUf, String anoInicio);
}
