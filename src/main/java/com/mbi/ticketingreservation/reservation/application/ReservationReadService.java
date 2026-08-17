package com.mbi.ticketingreservation.reservation.application;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationReadService {

    private static final EnumSet<ReservationStatus> ACTIVE_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public long getActiveSeatsForEvent(Long eventId) {
        return reservationRepository.sumSeatsByEventIdAndStatuses(eventId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getActiveSeatsByEventIds(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return reservationRepository.sumSeatsByEventIdsAndStatuses(eventIds, ACTIVE_STATUSES).stream()
                .collect(Collectors.toUnmodifiableMap(
                        ReservationRepository.ActiveReservedSeats::getEventId,
                        ReservationRepository.ActiveReservedSeats::getActiveReservedSeats));
    }
}
