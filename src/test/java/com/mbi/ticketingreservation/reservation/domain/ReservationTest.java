package com.mbi.ticketingreservation.reservation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTest {

    @Test
    void startsPendingAndCanBeConfirmed() {
        Reservation reservation = reservation(2);

        boolean changed = reservation.confirm();

        assertTrue(changed);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void pendingReservationCanBeCancelled() {
        Reservation reservation = reservation(1);

        boolean changed = reservation.cancel();

        assertTrue(changed);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void confirmedReservationCanBeCancelled() {
        Reservation reservation = reservation(1);
        reservation.confirm();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void cancellingAlreadyCancelledReservationIsIdempotent() {
        Reservation reservation = reservation(1);
        reservation.cancel();

        boolean changed = reservation.cancel();

        assertFalse(changed);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void confirmingAlreadyConfirmedReservationIsIdempotent() {
        Reservation reservation = reservation(1);
        reservation.confirm();

        boolean changed = reservation.confirm();

        assertFalse(changed);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void cancelledReservationCannotBeConfirmed() {
        Reservation reservation = reservation(1);
        reservation.cancel();

        assertThrows(IllegalStateException.class, reservation::confirm);
    }

    @Test
    void rejectsNonPositiveSeats() {
        assertThrows(IllegalArgumentException.class, () -> reservation(0));
    }

    private Reservation reservation(int seats) {
        return new Reservation(10L, 20L, seats);
    }
}
