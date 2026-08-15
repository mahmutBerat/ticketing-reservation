package com.mbi.ticketingreservation.event.application;

import java.time.Instant;

public record EventReservationDTO(
        long eventId,
        int capacity,
        boolean published,
        Instant startsAt
) {
}
