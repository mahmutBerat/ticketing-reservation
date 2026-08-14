package com.mbi.ticketingreservation.event.api;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record CreateEventRequest(
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String venue,

        Instant startsAt,
        Instant endsAt,

        @NotNull
        @Positive
        @Max(value = 10_000)
        Integer capacity
) {

    @AssertTrue(message = "startsAt must be before endsAt")
    public boolean isDateRangeValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }
}
