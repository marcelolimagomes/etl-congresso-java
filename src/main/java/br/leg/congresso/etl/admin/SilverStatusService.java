package br.leg.congresso.etl.admin;

import br.leg.congresso.etl.admin.dto.SilverStatusDTO;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import br.leg.congresso.etl.repository.silver.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço consulta de status da camada Silver.
 * Agrega contagens das entidades Silver para exibição no endpoint de monitoramento.
 */
@Service
@RequiredArgsConstructor
public class SilverStatusService {

    private final SilverCamaraProposicaoRepository camaraProposicaoRepository;
    private final SilverCamaraTramitacaoRepository camaraTramitacaoRepository;
    private final SilverSenadoMateriaRepository senadoMateriaRepository;
    private final SilverSenadoMovimentacaoRepository senadoMovimentacaoRepository;
    private final ProposicaoRepository proposicaoRepository;

    /**
     * Calcula e retorna as contagens atuais da camada Silver.
     *
     * @return snapshot do estado Silver no momento da consulta
     */
    @Transactional(readOnly = true)
    public SilverStatusDTO calcularStatus(Integer ano) {
        boolean comFiltroAno = ano != null;

        long camaraProposicoes   = comFiltroAno ? camaraProposicaoRepository.countByAno(ano) : camaraProposicaoRepository.count();
        long camaraTramitacoes   = comFiltroAno ? camaraTramitacaoRepository.countByProposicaoAno(ano) : camaraTramitacaoRepository.count();
        long senadoMaterias      = comFiltroAno ? senadoMateriaRepository.countByAno(ano) : senadoMateriaRepository.count();
        long senadoPendentes     = comFiltroAno
            ? senadoMateriaRepository.countPendentesEnriquecimentoByAno(ano)
            : senadoMateriaRepository.countByDetSiglaCasaIdentificacaoIsNull();
        long senadoMovimentacoes = comFiltroAno ? senadoMovimentacaoRepository.countByMateriaAno(ano) : senadoMovimentacaoRepository.count();

        long goldCamara = comFiltroAno
            ? proposicaoRepository.countByCasaAndAno(CasaLegislativa.CAMARA, ano)
            : proposicaoRepository.countByCasa(CasaLegislativa.CAMARA);
        long goldSenado = comFiltroAno
            ? proposicaoRepository.countByCasaAndAno(CasaLegislativa.SENADO, ano)
            : proposicaoRepository.countByCasa(CasaLegislativa.SENADO);

        return new SilverStatusDTO(
            ano,
            camaraProposicoes,
            camaraTramitacoes,
            senadoMaterias,
            senadoPendentes,
            senadoMovimentacoes,
            goldCamara,
            goldSenado
        );
    }
}
