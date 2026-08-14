package com.mbi.ticketingreservation.event.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record PublicEventQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @Size(max = 255) String q
) {

    @AssertTrue(message = "from must not be after to")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
