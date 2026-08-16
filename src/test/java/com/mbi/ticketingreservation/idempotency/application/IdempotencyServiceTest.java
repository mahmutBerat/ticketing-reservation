package com.mbi.ticketingreservation.idempotency.application;

import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.idempotency.domain.IdempotencyStatus;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import com.mbi.ticketingreservation.reservation.application.IdempotencyConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final Long ACTOR_ID = 3L;
    private static final String ENDPOINT = "/api/events/{eventId}/reservations";
    private static final String KEY = "request-key";
    private static final String REQUEST_HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plus(10, ChronoUnit.MINUTES);

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(idempotencyKeyRepository);
        ReflectionTestUtils.setField(idempotencyService, "ttl", Duration.ofMinutes(10));
    }

    @Test
    void findsActiveIdempotencyKey() {
        IdempotencyKey expectedKey = new IdempotencyKey(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, EXPIRES_AT);
        when(idempotencyKeyRepository.findByActorIdAndEndpointAndKey(
                ACTOR_ID, ENDPOINT, KEY)).thenReturn(Optional.of(expectedKey));

        Optional<IdempotencyKey> actualKey = idempotencyService.findActive(ACTOR_ID, ENDPOINT, KEY);

        assertEquals(Optional.of(expectedKey), actualKey);
        verify(idempotencyKeyRepository).findByActorIdAndEndpointAndKey(ACTOR_ID, ENDPOINT, KEY);
    }

    @Test
    void claimsProcessingIdempotencyKeyWithConfiguredTtl() {
        idempotencyService.create(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, NOW);

        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotencyKeyRepository).saveAndFlush(captor.capture());
        IdempotencyKey actualKey = captor.getValue();

        assertEquals(ACTOR_ID, actualKey.getActorId());
        assertEquals(ENDPOINT, actualKey.getEndpoint());
        assertEquals(KEY, actualKey.getKey());
        assertEquals(REQUEST_HASH, actualKey.getRequestHash());
        assertEquals(EXPIRES_AT, actualKey.getExpiresAt());
        assertEquals(IdempotencyStatus.PROCESSING, actualKey.getStatus());
    }

    @Test
    void physicallyDeletesExpiredKey() {
        IdempotencyKey idempotencyKey = new IdempotencyKey(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, NOW);

        idempotencyService.deleteExpired(idempotencyKey);

        verify(idempotencyKeyRepository).delete(idempotencyKey);
        verify(idempotencyKeyRepository).flush();
    }

    @Test
    void mapsConcurrentUniqueConstraintFailureToConflict() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(idempotencyKeyRepository).saveAndFlush(any(IdempotencyKey.class));

        assertThrows(
                IdempotencyConflictException.class,
                () -> idempotencyService.create(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, NOW));
    }

}
