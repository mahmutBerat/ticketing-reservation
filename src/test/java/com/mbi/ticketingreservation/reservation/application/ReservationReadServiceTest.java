package com.mbi.ticketingreservation.reservation.application;

import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;

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
}
