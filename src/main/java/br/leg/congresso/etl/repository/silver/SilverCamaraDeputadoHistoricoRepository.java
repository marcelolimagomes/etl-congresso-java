package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoHistorico;

/**
 * Repositório Silver — histórico de status de deputados da Câmara.
 * Tabela: silver.camara_deputado_historico
 */
@Repository
public interface SilverCamaraDeputadoHistoricoRepository
        extends JpaRepository<SilverCamaraDeputadoHistorico, UUID> {

    /**
     * Verifica se já existe registro histórico com a chave natural composta.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dataHora         data/hora do status
     * @param idLegislatura    ID da legislatura
     */
    boolean existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(
            String camaraDeputadoId, String dataHora, String idLegislatura);
}
