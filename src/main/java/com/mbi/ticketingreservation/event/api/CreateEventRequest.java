package com.mbi.ticketingreservation.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record CreateEventRequest(
        @Size(max = 255)
        @Schema(example = "Summer Concert") String title,

        @Size(max = 255)
        @Schema(example = "Main Hall") String venue,

        @Schema(example = "2030-06-01T18:00:00Z") Instant startsAt,
        @Schema(example = "2030-06-01T20:00:00Z") Instant endsAt,

        @NotNull
        @Positive
        @Max(value = 10_000)
        @Schema(example = "100") Integer capacity
) {

    @AssertTrue(message = "startsAt must be before endsAt")
    public boolean isDateRangeValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }
}
