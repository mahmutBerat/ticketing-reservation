package com.mbi.ticketingreservation.reservation.persistence;

import com.mbi.ticketingreservation.reservation.domain.Reservation;
import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByEventIdAndUserIdAndStatusIn(Long eventId, Long userId, Collection<ReservationStatus> statuses);

    @Query("""
            select coalesce(sum(reservation.seats), 0)
            from Reservation reservation
            where reservation.eventId = :eventId
              and reservation.status in :statuses
            """)
    long sumSeatsByEventIdAndStatuses(
            @Param("eventId") Long eventId,
            @Param("statuses") Collection<ReservationStatus> statuses);

}
