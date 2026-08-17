package com.mbi.ticketingreservation.event.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record EventResponse(
        Long id,
        Long ownerId,
        String title,
        String venue,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        @Schema(description = "Seats held by pending or confirmed reservations", example = "42")
        long activeReservedSeats,
        boolean published,
        long version,
        Instant createdAt
) {
}
