package br.leg.congresso.etl.pagegen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Renderizador Thymeleaf standalone (sem servlet container).
 * Carrega os templates de {@code classpath:templates/pages/}.
 */
@Slf4j
@Component
public class ThymeleafRenderer {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TemplateEngine engine;

    public ThymeleafRenderer() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/pages/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // desabilita cache para recarga em desenvolvimento

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /**
     * Renderiza o template informado com as variáveis fornecidas e retorna o HTML
     * resultante.
     *
     * @param templateName nome do template sem extensão (ex: "proposicao")
     * @param variables    variáveis disponíveis no contexto do template
     * @return HTML renderizado como string
     */
    public String render(String templateName, Map<String, Object> variables) {
        log.debug("Renderizando template '{}' com {} variáveis", templateName, variables.size());
        var context = new Context(PT_BR);
        var mergedVariables = new HashMap<>(variables);
        mergedVariables.putIfAbsent("globalHead", StaticPageGlobalHead.templateModel());
        context.setVariables(mergedVariables);
        return engine.process(templateName, context);
    }
}
