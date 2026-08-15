package com.mbi.ticketingreservation.reservation.application;

public class InvalidIdempotencyKeyException extends IllegalArgumentException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be a valid UUIDv4");
    }
}
