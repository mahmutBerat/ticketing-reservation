package com.mbi.ticketingreservation.reservation.api;

import com.mbi.ticketingreservation.common.security.SessionUser;
import com.mbi.ticketingreservation.common.security.SessionUserProvider;
import com.mbi.ticketingreservation.reservation.application.ReservationService;
import com.mbi.ticketingreservation.common.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;
    private final SessionUserProvider sessionUserProvider;

    @PostMapping("/events/{eventId}/reservations")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create a reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or idempotency key",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Capacity, idempotency or concurrency conflict",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservationResponse> create(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateReservationRequest request,
            @Parameter(description = "UUIDv4 key; a non-expired key cannot be reused",
                    required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(summary = "Confirm a reservation", description = "Returns the current reservation when it is already confirmed")
    public ReservationResponse confirm(@PathVariable Long reservationId, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return reservationService.confirm(
                reservationId, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Cancel a reservation", description = "Returns the current reservation when it is already cancelled")
    public ReservationResponse cancel(@PathVariable Long reservationId, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return reservationService.cancel(reservationId, sessionUser.userId(), sessionUser.isAdmin(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }
}
