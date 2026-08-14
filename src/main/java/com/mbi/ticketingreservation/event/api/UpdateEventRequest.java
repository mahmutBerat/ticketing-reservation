package com.mbi.ticketingreservation.event.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateEventRequest(
        @Size(max = 255) String title,
        @Size(max = 255) String venue,
        Instant startsAt,
        Instant endsAt,
        @NotNull @Positive Integer capacity
) {

    @AssertTrue(message = "startsAt must be before endsAt")
    public boolean isDateRangeValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }
}
