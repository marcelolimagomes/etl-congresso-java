package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorComissao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorComissaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorComissaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega comissões de senadores na camada Silver
 * (silver.senado_senador_comissao).
 * Deduplicação por (codigo_senador, codigo_comissao, data_inicio_participacao).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorComissaoLoader {

    private final SilverSenadoSenadorComissaoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorComissaoDTO.Comissao> comissoes, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || comissoes == null || comissoes.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorComissaoDTO.Comissao comissao : comissoes) {
            if (comissao.getCodigoComissao() == null || comissao.getCodigoComissao().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoComissaoAndDataInicioParticipacao(
                    codigoSenador, comissao.getCodigoComissao(), comissao.getDataInicio())) {
                repository.save(toEntity(codigoSenador, comissao, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado comissões senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorComissao toEntity(String codigoSenador,
            SenadoSenadorComissaoDTO.Comissao c, UUID jobId) {
        return SilverSenadoSenadorComissao.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoComissao(c.getCodigoComissao())
                .siglaComissao(c.getSiglaComissao())
                .nomeComissao(c.getNomeComissao())
                .cargo(c.getDescricaoParticipacao())
                .dataInicioParticipacao(c.getDataInicio())
                .dataTerminoParticipacao(c.getDataFim())
                .ativo(c.getIndicadorAtividade())
                .build();
    }
}
