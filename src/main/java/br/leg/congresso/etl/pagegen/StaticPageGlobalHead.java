package br.leg.congresso.etl.pagegen;

import java.util.Map;

/**
 * Configuração global de head para páginas estáticas geradas pelo Java.
 * Centraliza scripts e meta tags que também existem na aplicação Nuxt.
 */
public final class StaticPageGlobalHead {

    public static final String GOOGLE_ADSENSE_ACCOUNT = "ca-pub-2690072901856701";
    public static final String GOOGLE_ANALYTICS_MEASUREMENT_ID = "G-RR9S2KQ44D";
    public static final String ROBOTS_CONTENT = "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1";
    public static final String SITE_PRECONNECT_URL = "https://www.translegis.com.br";
    public static final String ADSENSE_PRECONNECT_URL = "https://pagead2.googlesyndication.com";
    public static final String ANALYTICS_PRECONNECT_URL = "https://www.googletagmanager.com";
    public static final String ADSENSE_SCRIPT_SRC = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client="
            + GOOGLE_ADSENSE_ACCOUNT;
    public static final String ANALYTICS_SCRIPT_SRC = "https://www.googletagmanager.com/gtag/js?id="
            + GOOGLE_ANALYTICS_MEASUREMENT_ID;
    public static final String ANALYTICS_BOOTSTRAP_SCRIPT = "window.dataLayer = window.dataLayer || [];\n"
            + "function gtag(){dataLayer.push(arguments);}\n"
            + "gtag('js', new Date());\n"
            + "gtag('config', '" + GOOGLE_ANALYTICS_MEASUREMENT_ID + "');";
    public static final String THEME_SYNC_SCRIPT = "(function(){try{var m=localStorage.getItem('nuxt-color-mode')||'light';"
            + "if(m==='dark'||(m==='system'&&window.matchMedia('(prefers-color-scheme:dark)').matches))"
            + "{document.documentElement.classList.add('dark');}}catch(e){}}());";

    private StaticPageGlobalHead() {
    }

    public static Map<String, String> templateModel() {
        return Map.of(
                "googleAdsenseAccount", GOOGLE_ADSENSE_ACCOUNT,
                "googleAnalyticsMeasurementId", GOOGLE_ANALYTICS_MEASUREMENT_ID,
                "robotsContent", ROBOTS_CONTENT,
                "sitePreconnectUrl", SITE_PRECONNECT_URL,
                "adsensePreconnectUrl", ADSENSE_PRECONNECT_URL,
                "analyticsPreconnectUrl", ANALYTICS_PRECONNECT_URL,
                "googleAdsenseScriptSrc", ADSENSE_SCRIPT_SRC,
                "googleAnalyticsScriptSrc", ANALYTICS_SCRIPT_SRC,
                "googleAnalyticsBootstrapScript", ANALYTICS_BOOTSTRAP_SCRIPT,
                "themeSyncScript", THEME_SYNC_SCRIPT);
    }

    public static void appendCommonHeadTags(StringBuilder sb) {
        sb.append("  <meta name=\"google-adsense-account\" content=\"")
                .append(GOOGLE_ADSENSE_ACCOUNT)
                .append("\" />\n")
                .append("  <meta name=\"robots\" content=\"")
                .append(ROBOTS_CONTENT)
                .append("\" />\n")
                .append("  <link rel=\"preconnect\" href=\"")
                .append(SITE_PRECONNECT_URL)
                .append("\" />\n")
                .append("  <link rel=\"preconnect\" href=\"")
                .append(ADSENSE_PRECONNECT_URL)
                .append("\" crossorigin=\"anonymous\" />\n")
                .append("  <link rel=\"preconnect\" href=\"")
                .append(ANALYTICS_PRECONNECT_URL)
                .append("\" crossorigin=\"anonymous\" />\n")
                .append("  <script src=\"")
                .append(ADSENSE_SCRIPT_SRC)
                .append("\" async crossorigin=\"anonymous\"></script>\n")
                .append("  <script src=\"")
                .append(ANALYTICS_SCRIPT_SRC)
                .append("\" async></script>\n")
                .append("  <script>")
                .append(ANALYTICS_BOOTSTRAP_SCRIPT)
                .append("</script>\n")
                .append("  <script>")
                .append(THEME_SYNC_SCRIPT)
                .append("</script>\n");
    }
}