package com.mbi.ticketingreservation.reservation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationTest {

    @Test
    void startsPendingAndCanBeConfirmed() {
        Reservation reservation = reservation(2);

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void pendingReservationCanBeCancelled() {
        Reservation reservation = reservation(1);

        reservation.cancel();

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
