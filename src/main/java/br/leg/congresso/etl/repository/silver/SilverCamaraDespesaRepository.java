package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDespesa;

/**
 * Repositório Silver — despesas CEAP da Câmara dos Deputados.
 * Tabela: silver.camara_despesa
 */
@Repository
public interface SilverCamaraDespesaRepository extends JpaRepository<SilverCamaraDespesa, UUID> {

    /**
     * Verifica se já existe despesa com a chave natural composta.
     *
     * @param camaraDeputadoId ID do deputado (ideCadastro no CSV)
     * @param codDocumento     código do documento (ideDocumento no CSV)
     * @param numDocumento     número do documento (txtNumero no CSV)
     * @param parcela          parcela (numParcela no CSV)
     */
    boolean existsByCamaraDeputadoIdAndCodDocumentoAndNumDocumentoAndParcela(
            String camaraDeputadoId, String codDocumento, String numDocumento, String parcela);

    java.util.List<SilverCamaraDespesa> findByCamaraDeputadoId(String camaraDeputadoId);
}
