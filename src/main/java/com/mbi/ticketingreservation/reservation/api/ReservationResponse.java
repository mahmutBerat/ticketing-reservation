package com.mbi.ticketingreservation.reservation.api;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ReservationResponse(
        @Schema(example = "1000") Long id,
        @Schema(example = "1000") Long eventId,
        @Schema(example = "3") Long userId,
        @Schema(example = "PENDING") ReservationStatus status,
        @Schema(example = "2") int seats,
        @Schema(example = "2026-08-16T12:00:00Z") Instant createdAt
) {
}
