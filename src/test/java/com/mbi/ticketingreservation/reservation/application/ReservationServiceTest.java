package com.mbi.ticketingreservation.reservation.application;

import com.mbi.ticketingreservation.audit.application.AuditService;
import com.mbi.ticketingreservation.event.application.EventReservationDTO;
import com.mbi.ticketingreservation.event.application.EventService;
import com.mbi.ticketingreservation.idempotency.application.IdempotencyService;
import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.reservation.api.CreateReservationRequest;
import com.mbi.ticketingreservation.reservation.api.ReservationMapper;
import com.mbi.ticketingreservation.reservation.api.ReservationResponse;
import com.mbi.ticketingreservation.reservation.domain.Reservation;
import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static com.mbi.ticketingreservation.reservation.application.ReservationService.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private EventService eventService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private AuditService auditService;

    private ReservationService reservationService;

    private static final Long RESERVATION_ID = 2000L;
    private static final Long EVENT_ID = 1000L;
    private static final Long CUSTOMER_ID = 3L;
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "unit-test";
    private static final Set<ReservationStatus> ACTIVE_STATUSES = EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(eventService, reservationRepository, idempotencyService, reservationMapper,
                auditService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPendingReservation() {
        CreateReservationRequest request = new CreateReservationRequest(2);
        EventReservationDTO event = new EventReservationDTO(EVENT_ID, 10, true, NOW.plus(1, ChronoUnit.DAYS));
        Reservation savedReservation = new Reservation(EVENT_ID, CUSTOMER_ID, request.seats());
        ReservationResponse expectedResponse = new ReservationResponse(null, EVENT_ID, CUSTOMER_ID, ReservationStatus.PENDING, request.seats(), null);

        when(idempotencyService.findActive(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(eventService.getForReservation(EVENT_ID)).thenReturn(event);
        when(reservationRepository.existsByEventIdAndUserIdAndStatusIn(EVENT_ID, CUSTOMER_ID, ACTIVE_STATUSES))
                .thenReturn(false);
        when(reservationRepository.sumSeatsByEventIdAndStatuses(EVENT_ID, ACTIVE_STATUSES)).thenReturn(4L);
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenReturn(savedReservation);
        when(reservationMapper.toResponse(savedReservation)).thenReturn(expectedResponse);

        ReservationResponse actualResponse = reservationService.create(EVENT_ID, request, CUSTOMER_ID, IDEMPOTENCY_KEY, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        verify(reservationRepository).saveAndFlush(any(Reservation.class));
        verify(auditService).saveRecord(CUSTOMER_ID, ReservationService.RESERVATION_CREATED, RESERVATION_RESOURCE,
                savedReservation.getId(), IP, USER_AGENT);
        verify(idempotencyService).saveIdempotencyKey(eq(CUSTOMER_ID), eq(CREATE_ENDPOINT), eq(IDEMPOTENCY_KEY),
                anyString(), eq(NOW));
    }

    @Test
    void rejectAlreadyIdempotentSavedAndExpiresAtNotPassed() {
        CreateReservationRequest request = new CreateReservationRequest(2);
        when(idempotencyService.findActive(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new IdempotencyKey(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY, createRequestHash(), Instant.now().plus(10, ChronoUnit.MINUTES))));

        assertThrows(IdempotencyConflictException.class, () ->
                reservationService.create(EVENT_ID, request, CUSTOMER_ID, IDEMPOTENCY_KEY, IP, USER_AGENT));
    }

    @Test
    void allowIdempotentKeySavedButExpiresAtPassed() {
        CreateReservationRequest request = new CreateReservationRequest(2);
        when(idempotencyService.findActive(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new IdempotencyKey(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY, createRequestHash(), Instant.now().minus(10, ChronoUnit.MINUTES))));

        assertThrows(IdempotencyConflictException.class, () ->
                reservationService.create(EVENT_ID, request, CUSTOMER_ID, IDEMPOTENCY_KEY, IP, USER_AGENT));
    }

    @Test
    void rejectsReservationWhenCustomerHasActiveReservation() {
        CreateReservationRequest request = new CreateReservationRequest(2);
        EventReservationDTO event = new EventReservationDTO(
                EVENT_ID, 10, true, NOW.plus(1, ChronoUnit.DAYS));

        when(idempotencyService.findActive(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(eventService.getForReservation(EVENT_ID)).thenReturn(event);
        when(reservationRepository.existsByEventIdAndUserIdAndStatusIn(
                EVENT_ID, CUSTOMER_ID, ACTIVE_STATUSES)).thenReturn(true);

        assertThrows(ActiveReservationExistsException.class, () ->
                reservationService.create(EVENT_ID, request, CUSTOMER_ID, IDEMPOTENCY_KEY, IP, USER_AGENT));
        verify(reservationRepository).existsByEventIdAndUserIdAndStatusIn(
                EVENT_ID, CUSTOMER_ID, ACTIVE_STATUSES);
    }

    @Test
    void rejectsReservationWhenRequestedSeatsExceedRemainingCapacity() {
        CreateReservationRequest request = new CreateReservationRequest(2);
        EventReservationDTO event = new EventReservationDTO(
                EVENT_ID, 10, true, NOW.plus(1, ChronoUnit.DAYS));

        when(idempotencyService.findActive(CUSTOMER_ID, CREATE_ENDPOINT, IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(eventService.getForReservation(EVENT_ID)).thenReturn(event);
        when(reservationRepository.existsByEventIdAndUserIdAndStatusIn(
                EVENT_ID, CUSTOMER_ID, ACTIVE_STATUSES)).thenReturn(false);
        when(reservationRepository.sumSeatsByEventIdAndStatuses(EVENT_ID, ACTIVE_STATUSES)).thenReturn(9L);

        assertThrows(EventCapacityExceededException.class, () ->
                reservationService.create(EVENT_ID, request, CUSTOMER_ID, IDEMPOTENCY_KEY, IP, USER_AGENT));
        verify(reservationRepository).sumSeatsByEventIdAndStatuses(EVENT_ID, ACTIVE_STATUSES);
    }

    @Test
    void confirmsPendingReservation() {
        Reservation reservation = new Reservation(EVENT_ID, CUSTOMER_ID, 2);
        reservation.setId(RESERVATION_ID);
        ReservationResponse expectedResponse = new ReservationResponse(
                RESERVATION_ID, EVENT_ID, CUSTOMER_ID, ReservationStatus.CONFIRMED, 2, null);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponse(reservation)).thenReturn(expectedResponse);

        ReservationResponse actualResponse = reservationService.confirm(RESERVATION_ID, CUSTOMER_ID, false, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(reservationRepository).saveAndFlush(reservation);
        verify(auditService).saveRecord(CUSTOMER_ID, RESERVATION_CONFIRMED, RESERVATION_RESOURCE, RESERVATION_ID, IP, USER_AGENT);
        verify(reservationMapper).toResponse(reservation);
    }

    @Test
    void cancelsPendingReservation() {
        Reservation reservation = new Reservation(EVENT_ID, CUSTOMER_ID, 2);
        reservation.setId(RESERVATION_ID);
        ReservationResponse expectedResponse = new ReservationResponse(
                RESERVATION_ID, EVENT_ID, CUSTOMER_ID, ReservationStatus.CANCELLED, 2, null);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponse(reservation)).thenReturn(expectedResponse);

        ReservationResponse actualResponse = reservationService.cancel(
                RESERVATION_ID, CUSTOMER_ID, false, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(reservationRepository).saveAndFlush(reservation);
        verify(auditService).saveRecord(CUSTOMER_ID, RESERVATION_CANCELLED, RESERVATION_RESOURCE, RESERVATION_ID, IP, USER_AGENT);
        verify(reservationMapper).toResponse(reservation);
    }


    String createRequestHash() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return HexFormat.of().formatHex(digest.digest("mockedRequestHash".getBytes(StandardCharsets.UTF_8)));
    }
}
