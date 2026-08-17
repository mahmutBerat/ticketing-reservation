package com.mbi.ticketingreservation.reservation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static com.mbi.ticketingreservation.reservation.application.ReservationService.MAX_SEATS_PER_REQUEST;

public record CreateReservationRequest(
        @NotNull
        @Positive
        @Max(MAX_SEATS_PER_REQUEST)
        @Schema(example = "2") Integer seats
) {
}
