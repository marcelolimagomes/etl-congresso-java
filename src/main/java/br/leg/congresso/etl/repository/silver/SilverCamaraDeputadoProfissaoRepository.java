package br.leg.congresso.etl.repository.silver;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoProfissao;

public interface SilverCamaraDeputadoProfissaoRepository
        extends JpaRepository<SilverCamaraDeputadoProfissao, UUID> {

    boolean existsByIdDeputadoAndTituloAndCodTipoProfissao(String idDeputado, String titulo, String codTipoProfissao);
}
