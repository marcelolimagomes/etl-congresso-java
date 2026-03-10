package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorProfissao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorProfissaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorProfissaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega profissões de senadores na camada Silver
 * (silver.senado_senador_profissao).
 * Deduplicação por (codigo_senador, codigo_profissao).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorProfissaoLoader {

    private final SilverSenadoSenadorProfissaoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorProfissaoDTO.Profissao> profissoes, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || profissoes == null || profissoes.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorProfissaoDTO.Profissao profissao : profissoes) {
            if (profissao.getCodigoProfissao() == null || profissao.getCodigoProfissao().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoProfissao(codigoSenador, profissao.getCodigoProfissao())) {
                repository.save(toEntity(codigoSenador, profissao, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado profissões senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorProfissao toEntity(String codigoSenador,
            SenadoSenadorProfissaoDTO.Profissao p, UUID jobId) {
        return SilverSenadoSenadorProfissao.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoProfissao(p.getCodigoProfissao())
                .descricaoProfissao(p.getDescricaoProfissao())
                .dataRegistro(p.getDataRegistro())
                .build();
    }
}
