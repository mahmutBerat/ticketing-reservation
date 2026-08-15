package com.mbi.ticketingreservation.reservation.application;

public class EventCapacityExceededException extends RuntimeException {

    public EventCapacityExceededException() {
        super("Requested seats exceed the event's remaining capacity");
    }

}
