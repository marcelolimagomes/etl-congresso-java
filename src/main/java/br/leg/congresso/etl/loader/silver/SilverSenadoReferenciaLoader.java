package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoRefAssunto;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefClasse;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefSigla;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoAutor;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoDecisao;
import br.leg.congresso.etl.domain.silver.SilverSenadoRefTipoSituacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRefDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefAssuntoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefClasseRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefSiglaRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoAutorRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoDecisaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoRefTipoSituacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader para tabelas de referência do Senado Federal.
 * Utiliza estratégia full-replace (deleteAll + saveAll) dentro de transação
 * única.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoReferenciaLoader {

    private final SilverSenadoRefTipoSituacaoRepository tipoSituacaoRepository;
    private final SilverSenadoRefTipoDecisaoRepository tipoDecisaoRepository;
    private final SilverSenadoRefTipoAutorRepository tipoAutorRepository;
    private final SilverSenadoRefSiglaRepository siglaRepository;
    private final SilverSenadoRefClasseRepository classeRepository;
    private final SilverSenadoRefAssuntoRepository assuntoRepository;

    @Transactional
    public int carregarTiposSituacao(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefTipoSituacao> entities = dtos.stream()
                .filter(d -> d.getCodigo() != null && !d.getCodigo().isBlank())
                .map(d -> SilverSenadoRefTipoSituacao.builder()
                        .etlJobId(jobId)
                        .codigo(d.getCodigo())
                        .descricao(d.getDescricao())
                        .build())
                .toList();
        tipoSituacaoRepository.deleteAll();
        tipoSituacaoRepository.saveAll(entities);
        log.debug("[Silver] Tipos de situação Senado carregados: {}", entities.size());
        return entities.size();
    }

    @Transactional
    public int carregarTiposDecisao(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefTipoDecisao> entities = dtos.stream()
                .filter(d -> d.getCodigo() != null && !d.getCodigo().isBlank())
                .map(d -> SilverSenadoRefTipoDecisao.builder()
                        .etlJobId(jobId)
                        .codigo(d.getCodigo())
                        .descricao(d.getDescricao())
                        .build())
                .toList();
        tipoDecisaoRepository.deleteAll();
        tipoDecisaoRepository.saveAll(entities);
        log.debug("[Silver] Tipos de decisão Senado carregados: {}", entities.size());
        return entities.size();
    }

    @Transactional
    public int carregarTiposAutor(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefTipoAutor> entities = dtos.stream()
                .filter(d -> d.getCodigo() != null && !d.getCodigo().isBlank())
                .map(d -> SilverSenadoRefTipoAutor.builder()
                        .etlJobId(jobId)
                        .codigo(d.getCodigo())
                        .descricao(d.getDescricao())
                        .build())
                .toList();
        tipoAutorRepository.deleteAll();
        tipoAutorRepository.saveAll(entities);
        log.debug("[Silver] Tipos de autor Senado carregados: {}", entities.size());
        return entities.size();
    }

    @Transactional
    public int carregarSiglas(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefSigla> entities = dtos.stream()
                .filter(d -> d.getSigla() != null && !d.getSigla().isBlank())
                .map(d -> SilverSenadoRefSigla.builder()
                        .etlJobId(jobId)
                        .sigla(d.getSigla())
                        .descricao(d.getDescricao())
                        .classe(d.getClasse())
                        .build())
                .toList();
        siglaRepository.deleteAll();
        siglaRepository.saveAll(entities);
        log.debug("[Silver] Siglas Senado carregadas: {}", entities.size());
        return entities.size();
    }

    @Transactional
    public int carregarClasses(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefClasse> entities = dtos.stream()
                .filter(d -> d.getCodigo() != null && !d.getCodigo().isBlank())
                .map(d -> SilverSenadoRefClasse.builder()
                        .etlJobId(jobId)
                        .codigo(d.getCodigo())
                        .descricao(d.getDescricao())
                        .classePai(d.getClassePai())
                        .build())
                .toList();
        classeRepository.deleteAll();
        classeRepository.saveAll(entities);
        log.debug("[Silver] Classes Senado carregadas: {}", entities.size());
        return entities.size();
    }

    @Transactional
    public int carregarAssuntos(List<SenadoRefDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        List<SilverSenadoRefAssunto> entities = dtos.stream()
                .filter(d -> d.getCodigo() != null && !d.getCodigo().isBlank())
                .map(d -> SilverSenadoRefAssunto.builder()
                        .etlJobId(jobId)
                        .codigo(d.getCodigo())
                        .assuntoGeral(d.getAssuntoGeral())
                        .assuntoEspecifico(d.getAssuntoEspecifico())
                        .build())
                .toList();
        assuntoRepository.deleteAll();
        assuntoRepository.saveAll(entities);
        log.debug("[Silver] Assuntos Senado carregados: {}", entities.size());
        return entities.size();
    }
}
