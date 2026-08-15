package com.mbi.ticketingreservation.reservation.application;

public class ActiveReservationExistsException extends RuntimeException {

    public ActiveReservationExistsException() {
        super("Customer already has an active reservation for this event");
    }
}
