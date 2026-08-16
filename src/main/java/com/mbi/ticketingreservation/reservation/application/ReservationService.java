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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    public static final int MAX_SEATS_PER_REQUEST = 10;

    static final String RESERVATION_CREATED = "RESERVATION_CREATED";
    static final String RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";
    static final String RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
    static final String RESERVATION_RESOURCE = "RESERVATION";
    static final String CREATE_ENDPOINT = "/api/events/{eventId}/reservations";
    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    private final EventService eventService;
    private final ReservationRepository reservationRepository;
    private final IdempotencyService idempotencyService;
    private final ReservationMapper reservationMapper;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public ReservationResponse create(Long eventId, CreateReservationRequest request, Long customerId, String idempotencyKey,
                                      String ip, String userAgent) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = getRequestHash(eventId, request.seats());
        Instant now = clock.instant();

        handleExistingIdempotencyKey(customerId, idempotencyKey, requestHash, now);

        EventReservationDTO event = eventService.getForReservation(eventId);
        validateReservation(event, customerId, request.seats());

        IdempotencyKey savedKey = idempotencyService.create(customerId, CREATE_ENDPOINT, idempotencyKey, requestHash, now);

        Reservation savedReservation = createReservation(eventId, request, customerId);

        ReservationResponse response = reservationMapper.toResponse(savedReservation);

        savedKey.complete();
        auditService.saveRecord(customerId, RESERVATION_CREATED, RESERVATION_RESOURCE, savedReservation.getId(), ip, userAgent);

        return response;
    }

    private Reservation createReservation(Long eventId, CreateReservationRequest request, Long customerId) {
        Reservation reservation = new Reservation(eventId, customerId, request.seats());
        return reservationRepository.saveAndFlush(reservation);
    }

    @Transactional
    public ReservationResponse confirm(Long reservationId, Long userId, boolean admin, String ip, String userAgent) {
        Reservation reservation = getReservationById(reservationId);
        isUserAuthorized(reservation, userId, admin);
        reservation.confirm();
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        auditService.saveRecord(userId, RESERVATION_CONFIRMED, RESERVATION_RESOURCE, reservationId, ip, userAgent);
        return reservationMapper.toResponse(savedReservation);
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId, Long userId, boolean admin, String ip, String userAgent) {
        Reservation reservation = getReservationById(reservationId);
        isUserAuthorized(reservation, userId, admin);
        reservation.cancel();
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        auditService.saveRecord(userId, RESERVATION_CANCELLED, RESERVATION_RESOURCE, reservationId, ip, userAgent);
        return reservationMapper.toResponse(savedReservation);
    }

    private Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
    }

    private void isUserAuthorized(Reservation reservation, Long userId, boolean admin) {
        if (!admin && !reservation.getUserId().equals(userId)) {
            throw new AccessDeniedException("Customer cannot modify another customer's reservation");
        }
    }

    private void validateReservation(EventReservationDTO event, Long customerId, int seats) {
        if (reservationRepository.existsByEventIdAndUserIdAndStatusIn(event.eventId(), customerId, EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))) {
            throw new ActiveReservationExistsException();
        }

        long activeSeats = reservationRepository.sumSeatsByEventIdAndStatuses(event.eventId(), EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
        if (seats > (event.capacity() - activeSeats)) {
            throw new EventCapacityExceededException();
        }
    }

    private void handleExistingIdempotencyKey(Long customerId, String idempotencyKey, String requestHash, Instant now) {
        IdempotencyKey existingKey = idempotencyService
                .findActive(customerId, CREATE_ENDPOINT, idempotencyKey)
                .orElse(null);
        if (existingKey == null) {
            log.debug("No any existingKey found with {}. Continue to make reservation.", idempotencyKey);
            return;
        }
        if (existingKey.isExpiredAt(now)) {
            log.debug("ExistingKey expired {}. Deleting and continue to make reservation.", idempotencyKey);
            idempotencyService.deleteExpired(existingKey);
            return;
        }
        String message = requestHash.equals(existingKey.getRequestHash())
                ? "Request with this Idempotency-Key has already been processed"
                : "Idempotency-Key was already used with a different request";
        throw new IdempotencyConflictException(message);
    }

    private void validateIdempotencyKey(String value) {
        if (value == null || !UUID_V4_PATTERN.matcher(value).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
    }

    private String getRequestHash(Long eventId, Integer seats) {
        String canonicalRequest = String.format("%s:%s", eventId, seats);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
