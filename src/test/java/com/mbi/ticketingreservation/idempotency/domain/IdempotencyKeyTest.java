package com.mbi.ticketingreservation.idempotency.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyKeyTest {

    private static final Instant EXPIRES_AT = Instant.parse("2030-01-01T12:10:00Z");
    private static final String REQUEST_HASH = "a".repeat(64);

    @Test
    void expiresAtConfiguredInstant() {
        IdempotencyKey key = key();

        assertFalse(key.isExpiredAt(EXPIRES_AT.minusSeconds(1)));
        assertTrue(key.isExpiredAt(EXPIRES_AT));
    }

    @Test
    void completesOnlyOnce() {
        IdempotencyKey key = key();

        key.complete();

        assertEquals(IdempotencyStatus.COMPLETED, key.getStatus());
        assertThrows(IllegalStateException.class, key::complete);
    }

    private IdempotencyKey key() {
        return new IdempotencyKey(3L, "/api/events/{eventId}/reservations", "key", REQUEST_HASH, EXPIRES_AT);
    }
}
