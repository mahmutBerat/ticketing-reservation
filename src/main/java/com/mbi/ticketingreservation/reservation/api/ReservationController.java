package com.mbi.ticketingreservation.reservation.api;

import com.mbi.ticketingreservation.common.security.SessionUser;
import com.mbi.ticketingreservation.common.security.SessionUserProvider;
import com.mbi.ticketingreservation.reservation.application.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SessionUserProvider sessionUserProvider;

    @PostMapping("/events/{eventId}/reservations")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> create(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest,
            UriComponentsBuilder uriBuilder
    ) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        ReservationResponse response = reservationService.create(eventId, request, sessionUser.userId(), idempotencyKey,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        URI location = uriBuilder.path("/api/reservations/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ReservationResponse confirm(@PathVariable Long reservationId, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return reservationService.confirm(
                reservationId, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ReservationResponse cancel(@PathVariable Long reservationId, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return reservationService.cancel(reservationId, sessionUser.userId(), sessionUser.isAdmin(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }
}
