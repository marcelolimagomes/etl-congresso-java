package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorCargo;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorCargoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorCargoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega cargos de senadores na camada Silver
 * (silver.senado_senador_cargo).
 * Deduplicação por (codigo_senador, codigo_cargo, data_inicio).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorCargoLoader {

    private final SilverSenadoSenadorCargoRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorCargoDTO.Cargo> cargos, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || cargos == null || cargos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorCargoDTO.Cargo cargo : cargos) {
            if (cargo.getCodigoCargo() == null || cargo.getCodigoCargo().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoCargoAndDataInicio(
                    codigoSenador, cargo.getCodigoCargo(), cargo.getDataInicio())) {
                repository.save(toEntity(codigoSenador, cargo, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado cargos senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorCargo toEntity(String codigoSenador,
            SenadoSenadorCargoDTO.Cargo c, UUID jobId) {
        return SilverSenadoSenadorCargo.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoCargo(c.getCodigoCargo())
                .descricaoCargo(c.getDescricaoCargo())
                .tipoCargo(c.getTipoCargo())
                .comissaoOuOrgao(c.getNomeOrgao())
                .dataInicio(c.getDataInicio())
                .dataFim(c.getDataFim())
                .build();
    }
}
