package com.mbi.ticketingreservation.idempotency.application;

import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import com.mbi.ticketingreservation.reservation.application.IdempotencyConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private @Value("${idempotency.ttl:10m}") Duration ttl;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<IdempotencyKey> findActive(Long userId, String endpoint, String key) {
        return idempotencyKeyRepository.findByActorIdAndEndpointAndKey(userId, endpoint, key);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteExpired(IdempotencyKey idempotencyKey) {
        idempotencyKeyRepository.delete(idempotencyKey);
        idempotencyKeyRepository.flush();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyKey create(Long userId, String endpoint, String key, String requestHash, Instant time) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(userId, endpoint, key, requestHash, time.plus(ttl));
        try {
            return idempotencyKeyRepository.saveAndFlush(idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyConflictException("Idempotency-Key is already in use");
        }
    }

}