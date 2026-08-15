package com.mbi.ticketingreservation.idempotency.application;

import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.idempotency.domain.IdempotencyStatus;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    }

    @Test
    void findsActiveIdempotencyKey() {
        IdempotencyKey expectedKey = new IdempotencyKey(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, EXPIRES_AT);
        when(idempotencyKeyRepository.findByActorIdAndEndpointAndKeyAndDeletedAtIsNull(
                ACTOR_ID, ENDPOINT, KEY)).thenReturn(Optional.of(expectedKey));

        Optional<IdempotencyKey> actualKey = idempotencyService.findActive(ACTOR_ID, ENDPOINT, KEY);

        assertEquals(Optional.of(expectedKey), actualKey);
        verify(idempotencyKeyRepository).findByActorIdAndEndpointAndKeyAndDeletedAtIsNull(ACTOR_ID, ENDPOINT, KEY);
    }

    @Test
    void savesCompletedIdempotencyKey() {
        IdempotencyKey expectedKey = new IdempotencyKey(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, EXPIRES_AT);
        expectedKey.complete();

        idempotencyService.saveIdempotencyKey(ACTOR_ID, ENDPOINT, KEY, REQUEST_HASH, NOW);

        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotencyKeyRepository).saveAndFlush(captor.capture());
        IdempotencyKey actualKey = captor.getValue();

        assertEquals(expectedKey.getActorId(), actualKey.getActorId());
        assertEquals(expectedKey.getEndpoint(), actualKey.getEndpoint());
        assertEquals(expectedKey.getKey(), actualKey.getKey());
        assertEquals(expectedKey.getRequestHash(), actualKey.getRequestHash());
        assertEquals(expectedKey.getExpiresAt(), actualKey.getExpiresAt());
        assertEquals(IdempotencyStatus.COMPLETED, actualKey.getStatus());
    }
}
