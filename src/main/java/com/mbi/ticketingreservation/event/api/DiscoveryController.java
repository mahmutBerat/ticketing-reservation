package com.mbi.ticketingreservation.event.api;

import com.mbi.ticketingreservation.event.application.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/public")
@RequiredArgsConstructor
@Tag(name = "Event discovery")
public class DiscoveryController {

    private final EventService eventService;

    @GetMapping
    @Operation(
            summary = "Search published events",
            description = "Returns published events ordered by start time. Optionally filters by an inclusive "
                    + "start-time range (`from` and `to`) and a case-insensitive title or venue search (`q`)."
    )
    public List<EventResponse> listPublic(@Valid @ModelAttribute PublicEventQuery query) {
        return eventService.listPublic(query);
    }
}
