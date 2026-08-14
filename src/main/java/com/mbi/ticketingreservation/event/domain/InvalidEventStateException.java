package com.mbi.ticketingreservation.event.domain;

public class InvalidEventStateException extends IllegalStateException {

    public InvalidEventStateException(String message) {
        super(message);
    }
}
