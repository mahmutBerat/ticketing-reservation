package com.mbi.ticketingreservation.dataintegrationtests;

import com.mbi.ticketingreservation.audit.persistence.AuditLogRepository;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import com.mbi.ticketingreservation.reservation.domain.Reservation;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SecurityIntegrationTest {

    private static final long ADMIN_ID = 1L;
    private static final long ORGANIZER_ID = 2L;
    private static final long CUSTOMER_ID = 3L;
    private static final Instant STARTS_AT = Instant.parse("2100-06-01T18:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2100-06-01T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanBusinessData() {
        auditLogRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void reservationRequiresAuthenticationAndCustomerRole() throws Exception {
        Event event = createPublishedEvent();
        var request = post("/api/events/{eventId}/reservations", event.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "00000000-0000-4000-8000-000000000301")
                .content("{\"seats\":1}");

        mockMvc.perform(request).andExpect(status().isUnauthorized());
        mockMvc.perform(request.with(jwtFor(CUSTOMER_ID, "CUSTOMER")))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyOrganizerOrAdminCanCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events")
                        .with(jwtFor(CUSTOMER_ID, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(10)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/events")
                        .with(jwtFor(ORGANIZER_ID, "ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ORGANIZER_ID));

        mockMvc.perform(post("/api/events")
                        .with(jwtFor(ADMIN_ID, "ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ADMIN_ID));
    }

    @Test
    void organizerCannotUpdateForeignEventButAdminCan() throws Exception {
        Event event = eventRepository.saveAndFlush(
                new Event(ORGANIZER_ID, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 10));

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .with(jwtFor(99L, "ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(20)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .with(jwtFor(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody(20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(20));
    }

    @Test
    void publicEventResponseIncludesPendingAndConfirmedReservedSeats() throws Exception {
        Event event = createPublishedEvent();
        Reservation pending = new Reservation(event.getId(), CUSTOMER_ID, 2);
        Reservation confirmed = new Reservation(event.getId(), CUSTOMER_ID, 3);
        confirmed.confirm();
        Reservation cancelled = new Reservation(event.getId(), CUSTOMER_ID, 4);
        cancelled.cancel();
        reservationRepository.saveAllAndFlush(List.of(pending, confirmed, cancelled));

        mockMvc.perform(get("/api/events/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activeReservedSeats").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private Event createPublishedEvent() {
        Event event = new Event(ORGANIZER_ID, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 10);
        event.publish();
        return eventRepository.saveAndFlush(event);
    }

    private RequestPostProcessor jwtFor(long userId, String role) {
        return jwt()
                .jwt(token -> token.subject(Long.toString(userId)).claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String eventBody(int capacity) {
        return """
                {
                  "title": "Concert",
                  "venue": "Main Hall",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "capacity": %d
                }
                """.formatted(STARTS_AT, ENDS_AT, capacity);
    }
}
