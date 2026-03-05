package br.leg.congresso.etl.service;

import br.leg.congresso.etl.repository.EtlLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço de lock distribuído baseado em tabela etl_lock.
 * Evita execução concorrente entre múltiplas instâncias da aplicação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlLockService {

    private final EtlLockRepository lockRepository;

    @Value("${etl.lock.ttl-minutes:180}")
    private long ttlMinutes;

    private final String instanceId = resolveInstanceId();

    @Transactional
    public boolean tryAcquire(String recurso) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime expiresAt = agora.plusMinutes(ttlMinutes);

        lockRepository.deleteExpired(agora);

        int inserted = lockRepository.tryAcquire(recurso, agora, instanceId, expiresAt);
        if (inserted == 1) {
            log.debug("Lock adquirido: recurso={}, holder={}", recurso, instanceId);
            return true;
        }

        int replaced = lockRepository.tryAcquireExpired(recurso, agora, instanceId, expiresAt);
        if (replaced == 1) {
            log.warn("Lock expirado recuperado: recurso={}, holder={}", recurso, instanceId);
            return true;
        }

        return false;
    }

    @Transactional
    public void release(String recurso) {
        int released = lockRepository.releaseOwned(recurso, instanceId);
        if (released == 1) {
            log.debug("Lock liberado: recurso={}, holder={}", recurso, instanceId);
        }
    }

    private String resolveInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "etl-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
