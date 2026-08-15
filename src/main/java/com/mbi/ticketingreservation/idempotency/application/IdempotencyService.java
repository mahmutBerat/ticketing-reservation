package com.mbi.ticketingreservation.idempotency.application;

import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private static final long IDEMPOTENCY_TTL_MINS = 10; // TODO move it to ENV

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<IdempotencyKey> findActive(Long userId, String endpoint, String key) {
        return idempotencyKeyRepository.findByActorIdAndEndpointAndKeyAndDeletedAtIsNull(userId, endpoint, key);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveIdempotencyKey(Long userId, String endpoint, String key, String requestHash, Instant time) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(userId, endpoint, key, requestHash, time.plus(IDEMPOTENCY_TTL_MINS, ChronoUnit.MINUTES));
        idempotencyKey.complete();
        idempotencyKeyRepository.saveAndFlush(idempotencyKey);
    }
}
