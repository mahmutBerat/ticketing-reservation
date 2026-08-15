package com.mbi.ticketingreservation.reservation.application;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException() {
        super("Reservation not found");
    }
}
