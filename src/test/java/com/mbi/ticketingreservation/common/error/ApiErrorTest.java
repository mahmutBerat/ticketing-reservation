package com.mbi.ticketingreservation.common.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorTest {

    @Test
    void defensivelyCopiesFieldErrors() {
        var error = new ApiError(ApiError.VALIDATION_FAILED, "Request is invalid", "trace-1", Instant.EPOCH, null);

        assertTrue(error.fieldErrors().isEmpty());
    }
}
