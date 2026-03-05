package br.leg.congresso.etl.extractor.camara.mapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoCSVRow;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoDTO;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Mapper MapStruct para converter DTOs da Câmara em entidades de domínio.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {CasaLegislativa.class, TipoProposicaoNormalizer.class})
public interface CamaraProposicaoMapper {

    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "casa", expression = "java(CasaLegislativa.CAMARA)")
    @Mapping(target = "sigla", source = "siglaTipo")
    @Mapping(target = "numero", expression = "java(parseIntSafe(row.getNumero()))")
    @Mapping(target = "ano", expression = "java(parseIntSafe(row.getAno()))")
    @Mapping(target = "tipo", expression = "java(TipoProposicaoNormalizer.normalizar(row.getSiglaTipo()))")
    @Mapping(target = "ementa", expression = "java(mergeEmenta(row.getEmenta(), row.getEmentaDetalhada()))")
    @Mapping(target = "dataApresentacao", expression = "java(parseDate(row.getDataApresentacao()))")
    @Mapping(target = "situacao", source = "ultimoStatusDescricaoSituacao")
    @Mapping(target = "despachoAtual", source = "ultimoStatusDespacho")
    @Mapping(target = "dataAtualizacao", expression = "java(parseDateTime(row.getUltimoStatusDataHora()))")
    @Mapping(target = "idOrigem", expression = "java(resolveIdOrigemCsv(row.getId(), row.getUri()))")
    @Mapping(target = "uriOrigem", source = "uri")
    @Mapping(target = "urlInteiroTeor", source = "urlInteiroTeor")
    @Mapping(target = "keywords", source = "keywords")
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "tramitacoes", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Proposicao csvRowToProposicao(CamaraProposicaoCSVRow row);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "casa", expression = "java(CasaLegislativa.CAMARA)")
    @Mapping(target = "sigla", source = "siglaTipo")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "ano", source = "ano")
    @Mapping(target = "tipo", expression = "java(TipoProposicaoNormalizer.normalizar(dto.getSiglaTipo()))")
    @Mapping(target = "ementa", source = "ementa")
    @Mapping(target = "dataApresentacao", expression = "java(parseDate(dto.getDataApresentacao()))")
    @Mapping(target = "situacao", source = "ultimoStatus.descricaoSituacao")
    @Mapping(target = "despachoAtual", source = "ultimoStatus.despacho")
    @Mapping(target = "dataAtualizacao", expression = "java(parseDateTime(dto.getUltimoStatus() != null ? dto.getUltimoStatus().getDataHora() : null))")
    @Mapping(target = "idOrigem", expression = "java(dto.getId() != null ? dto.getId().toString() : null)")
    @Mapping(target = "uriOrigem", source = "uri")
    @Mapping(target = "urlInteiroTeor", source = "urlInteiroTeor")
    @Mapping(target = "keywords", source = "keywords")
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "tramitacoes", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Proposicao apiDtoToProposicao(CamaraProposicaoDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proposicao", ignore = true)
    @Mapping(target = "sequencia", source = "sequencia")
    @Mapping(target = "dataHora", expression = "java(parseDateTime(dto.getDataHora()))")
    @Mapping(target = "siglaOrgao", source = "siglaOrgao")
    @Mapping(target = "descricaoOrgao", source = "descricaoOrgao")
    @Mapping(target = "descricaoTramitacao", source = "descricaoTramitacao")
    @Mapping(target = "descricaoSituacao", source = "descricaoSituacao")
    @Mapping(target = "despacho", source = "despacho")
    @Mapping(target = "ambito", source = "ambito")
    Tramitacao tramitacaoDtoToTramitacao(CamaraTramitacaoDTO dto);

    // ── Mapeamentos Silver (passthrough fiel à fonte) ──────────────────────────

    /**
     * Mapeia CSV row para Silver — passthrough puro, sem transformações normalizadoras.
     * Os campos ficam exatamente como vieram do CSV.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "etlJobId", ignore = true)
    @Mapping(target = "ingeridoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "goldSincronizado", constant = "false")
    @Mapping(target = "origemCarga", constant = "CSV")
    @Mapping(target = "tramitacoes", ignore = true)
    // Campos do CSV — direto sem parse
    @Mapping(target = "camaraId", source = "id")
    @Mapping(target = "siglaTipo", source = "siglaTipo")
    @Mapping(target = "codTipo", expression = "java(parseIntSafe(row.getCodTipo()))")
    @Mapping(target = "descricaoTipo", source = "descricaoTipo")
    @Mapping(target = "numero", expression = "java(parseIntSafe(row.getNumero()))")
    @Mapping(target = "ano", expression = "java(parseIntSafe(row.getAno()))")
    @Mapping(target = "ementa", source = "ementa")
    @Mapping(target = "ementaDetalhada", source = "ementaDetalhada")
    @Mapping(target = "keywords", source = "keywords")
    @Mapping(target = "dataApresentacao", source = "dataApresentacao")
    @Mapping(target = "uriOrgaoNumerador", source = "uriOrgaoNumerador")
    @Mapping(target = "uriPropAnterior", source = "uriPropAnterior")
    @Mapping(target = "uriPropPrincipal", source = "uriPropPrincipal")
    @Mapping(target = "uriPropPosterior", source = "uriPropPosterior")
    @Mapping(target = "urlInteiroTeor", source = "urlInteiroTeor")
    @Mapping(target = "urnFinal", source = "urnFinal")
    // ultimoStatus_* — agora com nomes corretos
    @Mapping(target = "ultimoStatusDataHora", source = "ultimoStatusDataHora")
    @Mapping(target = "ultimoStatusSequencia", expression = "java(parseIntSafe(row.getUltimoStatusSequencia()))")
    @Mapping(target = "ultimoStatusUriRelator", source = "ultimoStatusUriRelator")
    @Mapping(target = "ultimoStatusIdOrgao", expression = "java(parseIntSafe(row.getUltimoStatusIdOrgao()))")
    @Mapping(target = "ultimoStatusSiglaOrgao", source = "ultimoStatusSiglaOrgao")
    @Mapping(target = "ultimoStatusUriOrgao", source = "ultimoStatusUriOrgao")
    @Mapping(target = "ultimoStatusRegime", source = "ultimoStatusRegime")
    @Mapping(target = "ultimoStatusDescricaoTramitacao", source = "ultimoStatusDescricaoTramitacao")
    @Mapping(target = "ultimoStatusIdTipoTramitacao", source = "ultimoStatusIdTipoTramitacao")
    @Mapping(target = "ultimoStatusDescricaoSituacao", source = "ultimoStatusDescricaoSituacao")
    @Mapping(target = "ultimoStatusIdSituacao", expression = "java(parseIntSafe(row.getUltimoStatusIdSituacao()))")
    @Mapping(target = "ultimoStatusDespacho", source = "ultimoStatusDespacho")
    @Mapping(target = "ultimoStatusApreciacao", source = "ultimoStatusApreciacao")
    @Mapping(target = "ultimoStatusUrl", source = "ultimoStatusUrl")
    // Campos de API detalhe — não disponíveis no CSV
    @Mapping(target = "statusDataHora", ignore = true)
    @Mapping(target = "statusSequencia", ignore = true)
    @Mapping(target = "statusSiglaOrgao", ignore = true)
    @Mapping(target = "statusUriOrgao", ignore = true)
    @Mapping(target = "statusUriUltimoRelator", ignore = true)
    @Mapping(target = "statusRegime", ignore = true)
    @Mapping(target = "statusDescricaoTramitacao", ignore = true)
    @Mapping(target = "statusCodTipoTramitacao", ignore = true)
    @Mapping(target = "statusDescricaoSituacao", ignore = true)
    @Mapping(target = "statusCodSituacao", ignore = true)
    @Mapping(target = "statusDespacho", ignore = true)
    @Mapping(target = "statusUrl", ignore = true)
    @Mapping(target = "statusAmbito", ignore = true)
    @Mapping(target = "statusApreciacao", ignore = true)
    @Mapping(target = "uriAutores", ignore = true)
    @Mapping(target = "texto", ignore = true)
    @Mapping(target = "justificativa", ignore = true)
    SilverCamaraProposicao csvRowToSilver(CamaraProposicaoCSVRow row);

    /**
     * Mapeia DTO da API de detalhe para Silver — passthrough puro.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "etlJobId", ignore = true)
    @Mapping(target = "ingeridoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "goldSincronizado", constant = "false")
    @Mapping(target = "origemCarga", constant = "API")
    @Mapping(target = "tramitacoes", ignore = true)
    @Mapping(target = "camaraId", expression = "java(dto.getId() != null ? dto.getId().toString() : null)")
    @Mapping(target = "siglaTipo", source = "siglaTipo")
    @Mapping(target = "codTipo", source = "codTipo")
    @Mapping(target = "descricaoTipo", ignore = true)
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "ano", source = "ano")
    @Mapping(target = "ementa", source = "ementa")
    @Mapping(target = "ementaDetalhada", ignore = true)
    @Mapping(target = "keywords", source = "keywords")
    @Mapping(target = "dataApresentacao", source = "dataApresentacao")
    @Mapping(target = "uriOrgaoNumerador", ignore = true)
    @Mapping(target = "uriPropAnterior", ignore = true)
    @Mapping(target = "uriPropPrincipal", ignore = true)
    @Mapping(target = "uriPropPosterior", ignore = true)
    @Mapping(target = "urlInteiroTeor", source = "urlInteiroTeor")
    @Mapping(target = "urnFinal", ignore = true)
    // ultimoStatus_* — preenchidos via @AfterMapping
    @Mapping(target = "ultimoStatusDataHora", ignore = true)
    @Mapping(target = "ultimoStatusSequencia", ignore = true)
    @Mapping(target = "ultimoStatusUriRelator", ignore = true)
    @Mapping(target = "ultimoStatusIdOrgao", ignore = true)
    @Mapping(target = "ultimoStatusSiglaOrgao", ignore = true)
    @Mapping(target = "ultimoStatusUriOrgao", ignore = true)
    @Mapping(target = "ultimoStatusRegime", ignore = true)
    @Mapping(target = "ultimoStatusDescricaoTramitacao", ignore = true)
    @Mapping(target = "ultimoStatusIdTipoTramitacao", ignore = true)
    @Mapping(target = "ultimoStatusDescricaoSituacao", ignore = true)
    @Mapping(target = "ultimoStatusIdSituacao", ignore = true)
    @Mapping(target = "ultimoStatusDespacho", ignore = true)
    @Mapping(target = "ultimoStatusApreciacao", ignore = true)
    @Mapping(target = "ultimoStatusUrl", ignore = true)
    // Campos statusProposicao — preenchidos via @AfterMapping
    @Mapping(target = "statusDataHora", ignore = true)
    @Mapping(target = "statusSequencia", ignore = true)
    @Mapping(target = "statusSiglaOrgao", ignore = true)
    @Mapping(target = "statusUriOrgao", ignore = true)
    @Mapping(target = "statusUriUltimoRelator", ignore = true)
    @Mapping(target = "statusRegime", ignore = true)
    @Mapping(target = "statusDescricaoTramitacao", ignore = true)
    @Mapping(target = "statusCodTipoTramitacao", ignore = true)
    @Mapping(target = "statusDescricaoSituacao", ignore = true)
    @Mapping(target = "statusCodSituacao", ignore = true)
    @Mapping(target = "statusDespacho", ignore = true)
    @Mapping(target = "statusUrl", ignore = true)
    @Mapping(target = "statusAmbito", ignore = true)
    @Mapping(target = "statusApreciacao", ignore = true)
    @Mapping(target = "uriAutores", ignore = true)
    @Mapping(target = "texto", ignore = true)
    @Mapping(target = "justificativa", ignore = true)
    SilverCamaraProposicao apiDtoToSilver(CamaraProposicaoDTO dto);

    /**
     * Preenche campos de ultimoStatus e statusProposicao a partir do DTO da API
     * após o mapeamento principal, evitando repetição de null-checks.
     */
    @AfterMapping
    default void preencherUltimoStatus(CamaraProposicaoDTO dto, @MappingTarget SilverCamaraProposicao silver) {
        var status = dto.getUltimoStatus();
        if (status == null) return;

        // ultimoStatus_*
        silver.setUltimoStatusDataHora(status.getDataHora());
        silver.setUltimoStatusSequencia(parseIntSafe(status.getSequencia()));
        silver.setUltimoStatusSiglaOrgao(status.getSiglaOrgao());
        silver.setUltimoStatusUriOrgao(status.getUriOrgao());
        silver.setUltimoStatusRegime(status.getRegime());
        silver.setUltimoStatusDescricaoTramitacao(status.getDescricaoTramitacao());
        silver.setUltimoStatusIdTipoTramitacao(status.getCodTipoTramitacao());
        silver.setUltimoStatusDescricaoSituacao(status.getDescricaoSituacao());
        silver.setUltimoStatusIdSituacao(parseIntSafe(status.getCodSituacao()));
        silver.setUltimoStatusDespacho(status.getDespacho());
        silver.setUltimoStatusApreciacao(status.getApreciacao());
        silver.setUltimoStatusUrl(status.getUrl());

        // statusProposicao (mesma origem API)
        silver.setStatusDataHora(status.getDataHora());
        silver.setStatusSequencia(parseIntSafe(status.getSequencia()));
        silver.setStatusSiglaOrgao(status.getSiglaOrgao());
        silver.setStatusUriOrgao(status.getUriOrgao());
        silver.setStatusUriUltimoRelator(status.getUriUltimoRelator());
        silver.setStatusRegime(status.getRegime());
        silver.setStatusDescricaoTramitacao(status.getDescricaoTramitacao());
        silver.setStatusCodTipoTramitacao(status.getCodTipoTramitacao());
        silver.setStatusDescricaoSituacao(status.getDescricaoSituacao());
        silver.setStatusCodSituacao(parseIntSafe(status.getCodSituacao()));
        silver.setStatusDespacho(status.getDespacho());
        silver.setStatusUrl(status.getUrl());
        silver.setStatusAmbito(status.getAmbito());
        silver.setStatusApreciacao(status.getApreciacao());
    }

    default Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            // tenta apenas data (yyyy-MM-dd)
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    default LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                LocalDate date = LocalDate.parse(value.trim().substring(0, 10));
                return date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    default String mergeEmenta(String ementa, String ementaDetalhada) {
        if (ementa != null && !ementa.isBlank()) {
            return ementa;
        }
        return ementaDetalhada;
    }

    default String resolveIdOrigemCsv(String id, String uri) {
        if (id != null && !id.isBlank()) {
            return id.trim();
        }
        if (uri == null || uri.isBlank()) {
            return null;
        }

        String trimmed = uri.trim();
        int idx = trimmed.lastIndexOf('/');
        if (idx >= 0 && idx < trimmed.length() - 1) {
            String candidate = trimmed.substring(idx + 1).trim();
            return candidate.isEmpty() ? null : candidate;
        }
        return null;
    }
}
