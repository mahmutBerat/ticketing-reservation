package com.mbi.ticketingreservation.event.api;

import com.mbi.ticketingreservation.event.application.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse create(
            @Valid @RequestBody CreateEventRequest request, @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return eventService.create(request, Long.valueOf(jwt.getSubject()), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request,
                                @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpRequest) {
        return eventService.update(id, request, currentUserId(jwt), isAdmin(jwt), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, HttpServletRequest httpRequest) {
        return eventService.publish(id, currentUserId(jwt), isAdmin(jwt), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public List<EventResponse> list(@RequestParam(required = false) Long ownerId, @AuthenticationPrincipal Jwt jwt) {
        return eventService.list(ownerId, currentUserId(jwt), isAdmin(jwt));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return eventService.getById(id, currentUserId(jwt), isAdmin(jwt));
    }

    @GetMapping("/public")
    public List<EventResponse> listPublic(@Valid @ModelAttribute PublicEventQuery query) {
        return eventService.listPublic(query);
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
