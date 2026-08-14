package com.mbi.ticketingreservation.event.api;

import java.time.Instant;

public record EventResponse(
        Long id,
        Long ownerId,
        String title,
        String venue,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        boolean published,
        long version,
        Instant createdAt
) {
}
