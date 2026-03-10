package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoSenadorAfastado;
import br.leg.congresso.etl.extractor.senado.dto.SenadoAfastadoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoSenadorAfastadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega afastamentos de senadores na camada Silver
 * (silver.senado_senador_afastado).
 * Deduplicação por (codigo_senador, data_afastamento).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoSenadorAfastadoLoader {

    private final SilverSenadoSenadorAfastadoRepository repository;

    /**
     * Insere afastamentos inéditos; ignora os já existentes.
     *
     * @param parlamentares lista de parlamentares com afastamentos da API
     *                      /senador/afastados
     * @param jobId         UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<SenadoAfastadoDTO.Parlamentar> parlamentares, UUID jobId) {
        if (parlamentares == null || parlamentares.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoAfastadoDTO.Parlamentar parlamentar : parlamentares) {
            SenadoAfastadoDTO.IdentificacaoParlamentar idp = parlamentar.getIdentificacaoParlamentar();
            if (idp == null || idp.getCodigoParlamentar() == null || idp.getCodigoParlamentar().isBlank()) {
                continue;
            }

            List<SenadoAfastadoDTO.Afastamento> afastamentos = parlamentar.getAfastamentos() != null
                    ? parlamentar.getAfastamentos().getAfastamento()
                    : null;
            if (afastamentos == null || afastamentos.isEmpty()) {
                continue;
            }

            for (SenadoAfastadoDTO.Afastamento afastamento : afastamentos) {
                if (afastamento.getDataAfastamento() == null || afastamento.getDataAfastamento().isBlank()) {
                    continue;
                }

                if (!repository.existsByCodigoSenadorAndDataAfastamento(
                        idp.getCodigoParlamentar(), afastamento.getDataAfastamento())) {

                    repository.save(toEntity(idp, afastamento, jobId));
                    inseridos++;
                }
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado senadores afastados: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    // ── Conversão ─────────────────────────────────────────────────────────────

    private SilverSenadoSenadorAfastado toEntity(
            SenadoAfastadoDTO.IdentificacaoParlamentar idp,
            SenadoAfastadoDTO.Afastamento afastamento,
            UUID jobId) {

        return SilverSenadoSenadorAfastado.builder()
                .etlJobId(jobId)
                .codigoSenador(idp.getCodigoParlamentar())
                .nomeParlamentar(idp.getNomeParlamentar())
                .ufMandato(idp.getUfParlamentar())
                .motivoAfastamento(afastamento.getDescricaoCausaAfastamento())
                .dataAfastamento(afastamento.getDataAfastamento())
                .dataTerminoAfastamento(afastamento.getDataTerminoAfastamento())
                .build();
    }
}
