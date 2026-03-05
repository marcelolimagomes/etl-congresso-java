package br.leg.congresso.etl.extractor.senado.mapper;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.Tramitacao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.transformer.TipoProposicaoNormalizer;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Mapper MapStruct para converter DTOs do Senado em entidades de domínio.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {CasaLegislativa.class, TipoProposicaoNormalizer.class})
public interface SenadoMateriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "casa", expression = "java(CasaLegislativa.SENADO)")
    @Mapping(target = "sigla", source = "siglaSubtipoMateria")
    @Mapping(target = "numero", expression = "java(parseIntSafe(materia.getNumeroMateria()))")
    @Mapping(target = "ano", expression = "java(parseIntSafe(materia.getAnoMateria()))")
    @Mapping(target = "tipo", expression = "java(TipoProposicaoNormalizer.normalizar(materia.getSiglaSubtipoMateria()))")
    @Mapping(target = "ementa", source = "ementaMateria")
    @Mapping(target = "dataApresentacao", expression = "java(parseDate(materia.getDataApresentacao()))")
    @Mapping(target = "situacao", source = "situacaoAtualMateria.descricaoSituacao")
    @Mapping(target = "despachoAtual", ignore = true)
    @Mapping(target = "dataAtualizacao", expression = "java(parseDateTime(materia.getDataUltimaAtualizacao()))")
    @Mapping(target = "idOrigem", source = "codigoMateria")
    @Mapping(target = "uriOrigem", expression = "java(\"https://legis.senado.leg.br/dadosabertos/materia/\" + materia.getCodigoMateria())")
    @Mapping(target = "urlInteiroTeor", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "tramitacoes", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Proposicao materiaToProposicao(SenadoMateriaDTO.Materia materia);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proposicao", ignore = true)
    @Mapping(target = "sequencia", expression = "java(parseIntSafe(mov.getSequenciaMovimentacao()))")
    @Mapping(target = "dataHora", expression = "java(parseDateTime(mov.getDataMovimentacao()))")
    @Mapping(target = "siglaOrgao", source = "local.siglaLocal")
    @Mapping(target = "descricaoOrgao", source = "local.nomeLocal")
    @Mapping(target = "descricaoTramitacao", source = "descricaoMovimentacao")
    @Mapping(target = "descricaoSituacao", source = "descricaoSituacao")
    @Mapping(target = "despacho", source = "despacho")
    @Mapping(target = "ambito", source = "ambito")
    Tramitacao movimentacaoToTramitacao(SenadoMovimentacaoDTO.Movimentacao mov);

    // ── Mapeamento Silver (passthrough fiel à fonte) ─────────────────────────

    /**
     * Mapeia matéria da API de pesquisa para Silver — passthrough puro.
     * Campos complementares do detalhe ficam nulos e são preenchidos em fase de enriquecimento.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "etlJobId", ignore = true)
    @Mapping(target = "ingeridoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "goldSincronizado", constant = "false")
    @Mapping(target = "origemCarga", constant = "PESQUISA")
    @Mapping(target = "movimentacoes", ignore = true)
    // Campos de pesquisa
    @Mapping(target = "codigo", source = "codigoMateria")
    @Mapping(target = "sigla", source = "siglaSubtipoMateria")
    @Mapping(target = "numero", source = "numeroMateria")
    @Mapping(target = "ano", expression = "java(parseIntSafe(materia.getAnoMateria()))")
    @Mapping(target = "ementa", source = "ementaMateria")
    @Mapping(target = "autor", ignore = true)
    @Mapping(target = "data", source = "dataApresentacao")
    @Mapping(target = "urlDetalheMateria", expression = "java(materia.getCodigoMateria() != null ? \"https://legis.senado.leg.br/dadosabertos/materia/\" + materia.getCodigoMateria() : null)")
    @Mapping(target = "identificacaoProcesso",
        expression = "java(materia.getIdentificacaoMateria() != null ? materia.getIdentificacaoMateria().getSiglaCasaIdentificacaoMateria() : null)")
    @Mapping(target = "descricaoIdentificacao",
        expression = "java(materia.getIdentificacaoMateria() != null ? materia.getIdentificacaoMateria().getDescricaoIdentificacaoMateria() : null)")
    // Campos de detalhe — não disponíveis na pesquisa
    @Mapping(target = "detSiglaCasaIdentificacao", ignore = true)
    @Mapping(target = "detSiglaSubtipo", ignore = true)
    @Mapping(target = "detDescricaoSubtipo", ignore = true)
    @Mapping(target = "detDescricaoObjetivoProcesso", ignore = true)
    @Mapping(target = "detIndicadorTramitando", ignore = true)
    @Mapping(target = "detIndexacao", ignore = true)
    @Mapping(target = "detCasaIniciadora", ignore = true)
    @Mapping(target = "detIndicadorComplementar", ignore = true)
    @Mapping(target = "detNaturezaCodigo", ignore = true)
    @Mapping(target = "detNaturezaNome", ignore = true)
    @Mapping(target = "detNaturezaDescricao", ignore = true)
    @Mapping(target = "detSiglaCasaOrigem", ignore = true)
    @Mapping(target = "detClassificacoes", ignore = true)
    @Mapping(target = "detOutrasInformacoes", ignore = true)
    @Mapping(target = "urlTexto", ignore = true)
    @Mapping(target = "dataTexto", ignore = true)
    SilverSenadoMateria materiaToSilver(SenadoMateriaDTO.Materia materia);

    default Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    default LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String normalized = value.trim();
            // Formato Senado: dd/MM/yyyy ou yyyy-MM-dd
            if (normalized.contains("/")) {
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return LocalDate.parse(normalized.substring(0, Math.min(10, normalized.length())));
        } catch (DateTimeParseException e) { return null; }
    }

    default LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String normalized = value.trim();
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try { return parseDate(value) != null ? parseDate(value).atStartOfDay() : null; }
            catch (Exception ex) { return null; }
        }
    }
}
