package br.leg.congresso.etl.extractor.senado;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter adaptativo para a API do Senado Federal.
 * Ajusta dinamicamente a taxa de requisições com base nas respostas recebidas.
 */
@Slf4j
@Component
public class AdaptiveRateLimiter {

    private static final int MAX_RATE = 10;   // máximo permitido pelo Senado
    private static final int MIN_RATE = 2;    // mínimo seguro
    private static final int DEFAULT_RATE = 8;

    private final AtomicInteger currentLimit = new AtomicInteger(DEFAULT_RATE);

    /**
     * Retorna o limite atual em requisições por segundo.
     */
    public int getCurrentLimit() {
        return currentLimit.get();
    }

    /**
     * Aumenta o limite em 1 após resposta bem-sucedida, até MAX_RATE.
     */
    public void adjustOnSuccess() {
        int prev = currentLimit.getAndUpdate(v -> Math.min(v + 1, MAX_RATE));
        if (prev != currentLimit.get()) {
            log.debug("AdaptiveRateLimiter: limite aumentado para {}/s", currentLimit.get());
        }
    }

    /**
     * Reduz o limite em 2 após receber 429 Too Many Requests.
     */
    public void adjustOnTooManyRequests() {
        int prev = currentLimit.getAndUpdate(v -> Math.max(v - 2, MIN_RATE));
        log.warn("AdaptiveRateLimiter: 429 recebido, limite reduzido de {} para {}/s", prev, currentLimit.get());
    }

    /**
     * Reduz o limite em 3 após receber erro 5xx do servidor.
     */
    public void adjustOn5xx() {
        int prev = currentLimit.getAndUpdate(v -> Math.max(v - 3, MIN_RATE));
        log.warn("AdaptiveRateLimiter: erro 5xx, limite reduzido de {} para {}/s", prev, currentLimit.get());
    }

    /**
     * Calcula o delay mínimo em milissegundos entre requisições.
     */
    public long getDelayMs() {
        return 1000L / currentLimit.get();
    }

    /**
     * Reseta para o valor padrão.
     */
    public void reset() {
        currentLimit.set(DEFAULT_RATE);
        log.debug("AdaptiveRateLimiter: limite resetado para {}/s", DEFAULT_RATE);
    }
}
