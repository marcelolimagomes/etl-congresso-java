package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoEvento;

/**
 * Repositório Silver — eventos de deputados da Câmara.
 * Tabela: silver.camara_deputado_evento
 */
@Repository
public interface SilverCamaraDeputadoEventoRepository
        extends JpaRepository<SilverCamaraDeputadoEvento, UUID> {

    /**
     * Verifica se já existe evento com a chave natural composta.
     *
     * @param camaraDeputadoId ID do deputado
     * @param idEvento         ID do evento
     */
    boolean existsByCamaraDeputadoIdAndIdEvento(String camaraDeputadoId, String idEvento);
}
