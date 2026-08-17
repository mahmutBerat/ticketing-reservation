package com.mbi.ticketingreservation.reservation.application;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationReadServiceTest {

    private static final Long EVENT_ID = 1000L;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationReadService reservationReadService;

    @Test
    void returnsPendingAndConfirmedSeatCount() {
        EnumSet<ReservationStatus> activeStatuses =
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        when(reservationRepository.sumSeatsByEventIdAndStatuses(EVENT_ID, activeStatuses)).thenReturn(5L);

        long activeSeats = reservationReadService.getActiveSeatsForEvent(EVENT_ID);

        assertEquals(5L, activeSeats);
        verify(reservationRepository).sumSeatsByEventIdAndStatuses(EVENT_ID, activeStatuses);
    }

    @Test
    void returnsActiveSeatCountsForMultipleEvents() {
        Long secondEventId = 2000L;
        EnumSet<ReservationStatus> activeStatuses =
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        ReservationRepository.ActiveReservedSeats firstCount = activeSeats(EVENT_ID, 5L);
        ReservationRepository.ActiveReservedSeats secondCount = activeSeats(secondEventId, 3L);
        when(reservationRepository.sumSeatsByEventIdsAndStatuses(
                List.of(EVENT_ID, secondEventId), activeStatuses))
                .thenReturn(List.of(firstCount, secondCount));

        Map<Long, Long> activeSeats = reservationReadService.getActiveSeatsByEventIds(
                List.of(EVENT_ID, secondEventId));

        assertEquals(Map.of(EVENT_ID, 5L, secondEventId, 3L), activeSeats);
        verify(reservationRepository).sumSeatsByEventIdsAndStatuses(
                List.of(EVENT_ID, secondEventId), activeStatuses);
    }

    @Test
    void skipsRepositoryForEmptyEventList() {
        assertEquals(Map.of(), reservationReadService.getActiveSeatsByEventIds(List.of()));
    }

    private ReservationRepository.ActiveReservedSeats activeSeats(Long eventId, long count) {
        return new ReservationRepository.ActiveReservedSeats() {
            @Override
            public Long getEventId() {
                return eventId;
            }

            @Override
            public long getActiveReservedSeats() {
                return count;
            }
        };
    }
}
