package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorLicenca;
import br.leg.congresso.etl.extractor.senado.dto.SenadoSenadorLicencaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorLicencaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega licenças de senadores na camada Silver
 * (silver.senado_senador_licenca).
 * Deduplicação por (codigo_senador, codigo_licenca).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorLicencaLoader {

    private final SilverSenadoSenadorLicencaRepository repository;

    @Transactional
    public int carregar(String codigoSenador, List<SenadoSenadorLicencaDTO.Licenca> licencas, UUID jobId) {
        if (codigoSenador == null || codigoSenador.isBlank() || licencas == null || licencas.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoSenadorLicencaDTO.Licenca licenca : licencas) {
            if (licenca.getCodigoLicenca() == null || licenca.getCodigoLicenca().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoSenadorAndCodigoLicenca(codigoSenador, licenca.getCodigoLicenca())) {
                repository.save(toEntity(codigoSenador, licenca, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado licenças senador={}: {} novos registros", codigoSenador, inseridos);
        }
        return inseridos;
    }

    private SilverSenadoSenadorLicenca toEntity(String codigoSenador,
            SenadoSenadorLicencaDTO.Licenca l, UUID jobId) {
        return SilverSenadoSenadorLicenca.builder()
                .etlJobId(jobId)
                .codigoSenador(codigoSenador)
                .codigoLicenca(l.getCodigoLicenca())
                .dataInicio(l.getDataInicio())
                .dataFim(l.getDataFim())
                .motivo(l.getSiglaMotivoLicenca())
                .descricaoMotivo(l.getDescricaoMotivoLicenca())
                .build();
    }
}
