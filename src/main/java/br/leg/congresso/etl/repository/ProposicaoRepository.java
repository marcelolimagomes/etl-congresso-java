package br.leg.congresso.etl.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;

public interface ProposicaoRepository extends JpaRepository<Proposicao, UUID> {

    Optional<Proposicao> findByCasaAndSiglaAndNumeroAndAno(
            CasaLegislativa casa, String sigla, Integer numero, Integer ano);

    Optional<Proposicao> findByCasaAndIdOrigem(CasaLegislativa casa, String idOrigem);

    List<Proposicao> findByCasaAndAno(CasaLegislativa casa, Integer ano);

    List<Proposicao> findByTipoAndAno(TipoProposicao tipo, Integer ano);

    long countByCasa(CasaLegislativa casa);

    long countByCasaAndAno(CasaLegislativa casa, Integer ano);

    @Query("SELECT p.contentHash FROM Proposicao p WHERE p.casa = :casa AND p.sigla = :sigla AND p.numero = :numero AND p.ano = :ano")
    Optional<String> findContentHashByChaveNatural(
            @Param("casa") CasaLegislativa casa,
            @Param("sigla") String sigla,
            @Param("numero") Integer numero,
            @Param("ano") Integer ano);

    @Query("SELECT p FROM Proposicao p ORDER BY p.casa ASC, p.ano DESC, p.numero ASC")
    org.springframework.data.domain.Page<Proposicao> findAllForPageGen(
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Proposicao p WHERE p.ano = :ano ORDER BY p.casa ASC, p.numero ASC")
    org.springframework.data.domain.Page<Proposicao> findAllForPageGenByAno(
            @Param("ano") int ano,
            org.springframework.data.domain.Pageable pageable);

    interface SitemapProjection {
        CasaLegislativa getCasa();

        String getIdOrigem();

        LocalDateTime getAtualizadoEm();
    }

    @Query("SELECT p.casa as casa, p.idOrigem as idOrigem, p.atualizadoEm as atualizadoEm FROM Proposicao p WHERE p.idOrigem IS NOT NULL ORDER BY p.casa ASC, p.idOrigem ASC")
    org.springframework.data.domain.Page<SitemapProjection> findAllForSitemap(
            org.springframework.data.domain.Pageable pageable);

    long countByAno(int ano);

    @Modifying
    @Query(value = """
            INSERT INTO proposicao
                (id, casa, tipo, sigla, numero, ano, ementa, situacao, despacho_atual,
                 data_apresentacao, data_atualizacao, status_final, virou_lei,
                 id_origem, uri_origem, url_inteiro_teor, keywords, content_hash,
                 criado_em, atualizado_em)
            VALUES
                (gen_random_uuid(), :casa, :tipo, :sigla, :numero, :ano, :ementa,
                 :situacao, :despachoAtual, :dataApresentacao, :dataAtualizacao,
                 :statusFinal, false, :idOrigem, :uriOrigem, :urlInteiroTeor,
                 :keywords, :contentHash, NOW(), NOW())
            ON CONFLICT (casa, sigla, numero, ano)
            DO UPDATE SET
                id_origem         = EXCLUDED.id_origem,
                uri_origem        = EXCLUDED.uri_origem,
                ementa            = EXCLUDED.ementa,
                situacao          = EXCLUDED.situacao,
                despacho_atual    = EXCLUDED.despacho_atual,
                data_atualizacao  = EXCLUDED.data_atualizacao,
                status_final       = EXCLUDED.status_final,
                url_inteiro_teor  = EXCLUDED.url_inteiro_teor,
                keywords          = EXCLUDED.keywords,
                content_hash      = EXCLUDED.content_hash,
                atualizado_em     = NOW()
            WHERE proposicao.content_hash IS DISTINCT FROM EXCLUDED.content_hash
               OR proposicao.id_origem IS DISTINCT FROM EXCLUDED.id_origem
               OR proposicao.uri_origem IS DISTINCT FROM EXCLUDED.uri_origem
               OR proposicao.url_inteiro_teor IS DISTINCT FROM EXCLUDED.url_inteiro_teor
               OR proposicao.keywords IS DISTINCT FROM EXCLUDED.keywords
            """, nativeQuery = true)
    int upsert(
            @Param("casa") String casa,
            @Param("tipo") String tipo,
            @Param("sigla") String sigla,
            @Param("numero") Integer numero,
            @Param("ano") Integer ano,
            @Param("ementa") String ementa,
            @Param("situacao") String situacao,
            @Param("despachoAtual") String despachoAtual,
            @Param("dataApresentacao") LocalDate dataApresentacao,
            @Param("dataAtualizacao") LocalDateTime dataAtualizacao,
            @Param("statusFinal") String statusFinal,
            @Param("idOrigem") String idOrigem,
            @Param("uriOrigem") String uriOrigem,
            @Param("urlInteiroTeor") String urlInteiroTeor,
            @Param("keywords") String keywords,
            @Param("contentHash") String contentHash);
}
