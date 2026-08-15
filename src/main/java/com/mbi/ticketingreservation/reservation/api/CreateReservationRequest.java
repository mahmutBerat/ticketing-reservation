package com.mbi.ticketingreservation.reservation.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static com.mbi.ticketingreservation.reservation.application.ReservationService.MAX_SEATS_PER_REQUEST;

public record CreateReservationRequest(
        @NotNull
        @Positive
        @Max(MAX_SEATS_PER_REQUEST)
        Integer seats
) {
}
