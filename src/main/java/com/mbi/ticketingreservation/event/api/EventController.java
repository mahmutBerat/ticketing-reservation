package com.mbi.ticketingreservation.event.api;

import com.mbi.ticketingreservation.common.security.SessionUser;
import com.mbi.ticketingreservation.common.security.SessionUserProvider;
import com.mbi.ticketingreservation.event.application.EventService;
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
public class EventController {

    private final EventService eventService;
    private final SessionUserProvider sessionUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse create(
            @Valid @RequestBody CreateEventRequest request,
            HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.create(request, sessionUser.userId(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request,
                                HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.update(
                id, request, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse publish(@PathVariable Long id, HttpServletRequest httpRequest) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.publish(id, sessionUser.userId(), sessionUser.isAdmin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public List<EventResponse> list(@RequestParam(required = false) Long ownerId) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.list(ownerId, sessionUser.userId(), sessionUser.isAdmin());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse getById(@PathVariable Long id) {
        SessionUser sessionUser = sessionUserProvider.getSessionUser();
        return eventService.getById(id, sessionUser.userId(), sessionUser.isAdmin());
    }
}
