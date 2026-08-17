package com.mbi.ticketingreservation.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record PublicEventQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @Size(max = 255) String q,
        @Min(0) @Schema(defaultValue = "0", minimum = "0") Integer page,
        @Min(1) @Max(100) @Schema(defaultValue = "20", minimum = "1", maximum = "100") Integer size
) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;

    public PublicEventQuery {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    public PublicEventQuery(Instant from, Instant to, String q) {
        this(from, to, q, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    @AssertTrue(message = "from must not be after to")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
