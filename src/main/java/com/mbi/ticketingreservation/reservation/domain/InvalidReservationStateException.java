package com.mbi.ticketingreservation.reservation.domain;

public class InvalidReservationStateException extends IllegalStateException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}
