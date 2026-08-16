package com.mbi.ticketingreservation.event.application;

public class EventCapacityBelowActiveReservationsException extends RuntimeException {

    public EventCapacityBelowActiveReservationsException() {
        super("Event capacity cannot be lower than its active reserved seats");
    }
}
