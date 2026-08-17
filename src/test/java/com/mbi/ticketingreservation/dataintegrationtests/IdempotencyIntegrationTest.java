package com.mbi.ticketingreservation.dataintegrationtests;

import com.mbi.ticketingreservation.audit.persistence.AuditLogRepository;
import com.mbi.ticketingreservation.auth.domain.Role;
import com.mbi.ticketingreservation.auth.domain.User;
import com.mbi.ticketingreservation.auth.persistence.UserRepository;
import com.mbi.ticketingreservation.common.error.ApiError;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class IdempotencyIntegrationTest {

    private static final Long ORGANIZER_ID = 2L;
    private static final String ENDPOINT = "/api/events/{eventId}/reservations";
    private static final Instant STARTS_AT = Instant.parse("2100-06-01T18:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(2, ChronoUnit.HOURS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

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
    void repeatedKeyDoesNotCreateAnotherReservation() throws Exception {
        Event event = createPublishedEvent(5);
        User customer = createCustomer();
        String key = "00000000-0000-4000-8000-000000000201";

        postReservation(event.getId(), customer.getId(), key, 1)
                .andExpect(status().isCreated());

        postReservation(event.getId(), customer.getId(), key, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiError.IDEMPOTENCY_KEY_REUSED));

        assertCounts(event.getId(), 1, 1, 1);
    }

    @Test
    void repeatedKeyWithDifferentPayloadReturnsConflict() throws Exception {
        Event event = createPublishedEvent(5);
        User customer = createCustomer();
        String key = "00000000-0000-4000-8000-000000000202";

        postReservation(event.getId(), customer.getId(), key, 1)
                .andExpect(status().isCreated());

        postReservation(event.getId(), customer.getId(), key, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiError.IDEMPOTENCY_KEY_REUSED));

        assertCounts(event.getId(), 1, 1, 1);
    }

    @Test
    void expiredKeyIsPhysicallyReplaced() throws Exception {
        Event event = createPublishedEvent(5);
        User customer = createCustomer();
        String key = "00000000-0000-4000-8000-000000000203";
        IdempotencyKey expired = new IdempotencyKey(
                customer.getId(), ENDPOINT, key, "a".repeat(64), Instant.now().minusSeconds(1));
        expired.complete();
        idempotencyKeyRepository.saveAndFlush(expired);

        postReservation(event.getId(), customer.getId(), key, 1)
                .andExpect(status().isCreated());

        assertEquals(1L, idempotencyKeyRepository.count());
        IdempotencyKey replacement = idempotencyKeyRepository.findByActorIdAndEndpointAndKey(
                customer.getId(), ENDPOINT, key).orElseThrow();
        assertTrue(replacement.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void failedReservationRollbacksIdempotencyKey() throws Exception {
        Event event = createPublishedEvent(1);
        User customer = createCustomer();
        String key = "00000000-0000-4000-8000-000000000204";

        postReservation(event.getId(), customer.getId(), key, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiError.EVENT_CAPACITY_EXCEEDED));

        assertEquals(0L, idempotencyKeyRepository.count());
        assertEquals(0L, reservationsFor(event.getId()));
    }

    @Test
    void tenConcurrentRequestsWithSameKeyCreateOneReservation() throws Exception {
        Event event = createPublishedEvent(10);
        User customer = createCustomer();
        String key = "00000000-0000-4000-8000-000000000205";
        int requestCount = 10;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<MvcResult>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent request");
                    }
                    return postReservation(event.getId(), customer.getId(), key, 1).andReturn();
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS), "All requests should be ready");
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                statuses.add(future.get(15, TimeUnit.SECONDS).getResponse().getStatus());
            }

            assertEquals(1L, statuses.stream().filter(status -> status == 201).count());
            assertEquals(9L, statuses.stream().filter(status -> status == 409).count());
        }

        assertCounts(event.getId(), 1, 1, 1);
    }

    private ResultActions postReservation(Long eventId, Long customerId, String key, int seats) throws Exception {
        return mockMvc.perform(post("/api/events/{eventId}/reservations", eventId)
                .with(customerJwt(customerId))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content("{\"seats\":" + seats + "}"));
    }

    private void assertCounts(Long eventId, long reservations, long idempotencyKeys, long audits) {
        assertEquals(reservations, reservationsFor(eventId));
        assertEquals(idempotencyKeys, idempotencyKeyRepository.count());
        assertEquals(audits, auditLogRepository.findAll().stream()
                .filter(log -> "RESERVATION".equals(log.getResourceType()))
                .filter(log -> reservationRepository.findById(log.getResourceId())
                        .map(reservation -> eventId.equals(reservation.getEventId()))
                        .orElse(false))
                .count());
    }

    private long reservationsFor(Long eventId) {
        return reservationRepository.findAll().stream()
                .filter(reservation -> eventId.equals(reservation.getEventId()))
                .count();
    }

    private Event createPublishedEvent(int capacity) {
        Event event = new Event(ORGANIZER_ID, "Concert", "Main Hall", STARTS_AT, ENDS_AT, capacity);
        event.publish();
        return eventRepository.saveAndFlush(event);
    }

    private User createCustomer() {
        return userRepository.saveAndFlush(new User(
                "customer-" + UUID.randomUUID() + "@ticketing.local",
                "unused-password-hash",
                Set.of(Role.CUSTOMER)));
    }

    private RequestPostProcessor customerJwt(Long customerId) {
        return jwt()
                .jwt(token -> token.subject(customerId.toString()).claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
