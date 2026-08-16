package com.mbi.ticketingreservation.reservation.application;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class ReservationReadService {

    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public long getActiveSeatsForEvent(Long eventId) {
        return reservationRepository.sumSeatsByEventIdAndStatuses(eventId, EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    }
}
