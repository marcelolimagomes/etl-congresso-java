package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenador;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorDetalheDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorListaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega senadores do Senado Federal na camada Silver (silver.senado_senador).
 *
 * <p>
 * Operações:
 * <ul>
 * <li>{@link #carregar} — insere a partir da lista do endpoint
 * /senador/lista/atual.</li>
 * <li>{@link #carregarDetalhe} — complementa os campos det_* do endpoint
 * /senador/{codigo}.</li>
 * </ul>
 *
 * <p>
 * Deduplicação por codigo_senador. Registros já existentes são ignorados na
 * carga da lista.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorLoader {

    private final SilverSenadoSenadorRepository repository;

    /**
     * Insere senadores inéditos; ignora os já existentes.
     *
     * @param parlamentares lista de parlamentares da API /senador/lista/atual
     * @param jobId         UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<SenadoSenadorListaDTO.Parlamentar> parlamentares, UUID jobId) {
        if (parlamentares == null || parlamentares.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorListaDTO.Parlamentar p : parlamentares) {
            SenadoSenadorListaDTO.IdentificacaoParlamentar id = p.getIdentificacaoParlamentar();
            if (id == null || id.getCodigoParlamentar() == null || id.getCodigoParlamentar().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenador(id.getCodigoParlamentar())) {
                repository.save(parlamentarToEntity(p, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado senadores: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    /**
     * Complementa os campos det_* de um senador já existente.
     * Operação de update — não insere senadores novos.
     *
     * @param codigoSenador código do senador
     * @param dto           DTO do endpoint /senador/{codigo}
     * @param jobId         UUID do job ETL corrente
     * @return {@code true} se o registro foi atualizado
     */
    @Transactional
    public boolean carregarDetalhe(String codigoSenador, SenadoSenadorDetalheDTO dto, UUID jobId) {
        if (codigoSenador == null || dto == null || dto.getDetalheParlamentar() == null) {
            return false;
        }

        return repository.findByCodigoSenador(codigoSenador).map(senador -> {
            atualizarDet(senador, dto);
            senador.setEtlJobId(jobId);
            repository.save(senador);
            log.debug("[Silver] Senado senador detalhe atualizado: codigo={}", codigoSenador);
            return true;
        }).orElse(false);
    }

    // ── Conversão ─────────────────────────────────────────────────────────────

    private SilverSenadoSenador parlamentarToEntity(SenadoSenadorListaDTO.Parlamentar p, UUID jobId) {
        SenadoSenadorListaDTO.IdentificacaoParlamentar idp = p.getIdentificacaoParlamentar();
        SenadoSenadorListaDTO.Mandato mandato = p.getMandato();

        String sexo = null;
        if (idp.getSexoParlamentar() != null && !idp.getSexoParlamentar().isBlank()) {
            sexo = String.valueOf(idp.getSexoParlamentar().charAt(0)).toUpperCase();
        }

        String participacao = null;
        if (mandato != null && mandato.getDescricaoParticipacao() != null) {
            participacao = mandato.getDescricaoParticipacao().startsWith("S") ? "S" : "T";
        }

        return SilverSenadoSenador.builder()
                .etlJobId(jobId)
                .codigoSenador(idp.getCodigoParlamentar())
                .nomeParlamentar(idp.getNomeParlamentar())
                .nomeCivil(idp.getNomeCompletoParlamentar())
                .sexo(sexo)
                .ufParlamentar(idp.getUfParlamentar())
                .participacao(participacao)
                .siglaPartidoParlamentar(idp.getSiglaPartidoParlamentar())
                .dataDesignacao(mandato != null ? mandato.getDataDesignacao() : null)
                .codigoLegislatura(mandato != null ? mandato.getCodigoLegislatura() : null)
                .build();
    }

    private void atualizarDet(SilverSenadoSenador senador, SenadoSenadorDetalheDTO dto) {
        SenadoSenadorDetalheDTO.DetalheParlamentar dp = dto.getDetalheParlamentar();
        if (dp == null || dp.getParlamentar() == null)
            return;

        SenadoSenadorDetalheDTO.Parlamentar p = dp.getParlamentar();
        SenadoSenadorDetalheDTO.IdentificacaoParlamentar idp = p.getIdentificacaoParlamentar();
        SenadoSenadorDetalheDTO.DadosBasicosParlamentar dados = p.getDadosBasicosParlamentar();
        SenadoSenadorDetalheDTO.OutrasInformacoes outras = p.getOutrasInformacoes();

        if (idp != null) {
            senador.setDetNomeCompleto(idp.getNomeCompletoParlamentar());
            senador.setDetContatoEmail(idp.getEmailParlamentar());
            senador.setDetUrlFoto(idp.getUrlFotoParlamentar());
            senador.setDetUrlPaginaParlamentar(idp.getUrlPaginaParlamentar());
            senador.setDetPagina(idp.getUrlPaginaParticular());
        }

        if (dados != null) {
            senador.setDetDataNascimento(dados.getDataNascimento());
            senador.setDetLocalNascimento(dados.getNaturalidade());
            senador.setDetEstadoCivil(dados.getEstadoCivil());
            senador.setDetEscolaridade(dados.getEscolaridade());
        }

        if (outras != null) {
            senador.setDetFacebook(outras.getFacebook());
            senador.setDetTwitter(outras.getTwitter());
        }
    }
}
