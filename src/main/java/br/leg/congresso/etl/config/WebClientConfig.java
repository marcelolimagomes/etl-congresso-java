package br.leg.congresso.etl.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    /** Buffer de 200 MB para downloads de CSV */
    private static final int MAX_BUFFER_SIZE = 200 * 1024 * 1024;

    @Bean("camaraWebClient")
    public WebClient camaraWebClient(
            @Value("${etl.camara.base-url}") String baseUrl,
            @Value("${etl.camara.timeout-seconds:120}") int timeoutSeconds) {
        return buildWebClient(baseUrl, timeoutSeconds);
    }

    @Bean("camaraCsvWebClient")
    public WebClient camaraCsvWebClient(
            @Value("${etl.camara.csv-base-url}") String baseUrl) {
        return buildWebClient(baseUrl, 300); // 5 min para downloads grandes
    }

    @Bean("senadoWebClient")
    public WebClient senadoWebClient(
            @Value("${etl.senado.base-url}") String baseUrl,
            @Value("${etl.senado.timeout-seconds:60}") int timeoutSeconds) {
        return buildWebClient(baseUrl, timeoutSeconds);
    }

    private WebClient buildWebClient(String baseUrl, int timeoutSeconds) {
        HttpClient httpClient = HttpClient.create()
            .followRedirect(true)                                        // segue redirects HTTP → HTTPS
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(timeoutSeconds))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
            .build();

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.ACCEPT, "*/*")
            .defaultHeader(HttpHeaders.USER_AGENT, "etl-congresso/1.0 (dados-abertos)")
            .build();
    }
}
