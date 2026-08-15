package com.mbi.ticketingreservation.reservation.api;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long eventId,
        Long userId,
        ReservationStatus status,
        int seats,
        Instant createdAt
) {
}
