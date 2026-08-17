package com.mbi.ticketingreservation.event.api;

import com.mbi.ticketingreservation.common.security.SessionUser;
import com.mbi.ticketingreservation.common.security.SessionUserProvider;
import com.mbi.ticketingreservation.event.application.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;
    private final SessionUserProvider sessionUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create a draft event")
    public EventResponse create(
            @Valid @RequestBody CreateEventRequest request,
            HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.create(request, sessionUser.userId(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Update an event")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request,
                                HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.update(
                id, request, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Publish an event")
    public EventResponse publish(@PathVariable Long id, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.publish(id, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "List managed events")
    public List<EventResponse> list(@RequestParam(required = false) Long ownerId) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.list(ownerId, sessionUser.userId(), sessionUser.isAdmin());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get a managed event")
    public EventResponse getById(@PathVariable Long id) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.getById(id, sessionUser.userId(), sessionUser.isAdmin());
    }
}
