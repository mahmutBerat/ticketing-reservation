package com.mbi.ticketingreservation.reservation.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_id_generator")
    @SequenceGenerator(name = "reservation_id_generator", sequenceName = "reservations_seq", allocationSize = 1)
    @Setter
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(nullable = false)
    private int seats;

    @Version
    @Column(nullable = false)
    private long version;

    public Reservation(Long eventId, Long userId, int seats) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (seats <= 0) {
            throw new IllegalArgumentException("seats must be greater than zero");
        }
        this.seats = seats;
        this.status = ReservationStatus.PENDING;
    }

    public boolean confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            return false;
        }
        if (status != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException("Only a pending reservation can be confirmed");
        }
        status = ReservationStatus.CONFIRMED;
        return true;
    }

    public boolean cancel() {
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        status = ReservationStatus.CANCELLED;
        return true;
    }

}
