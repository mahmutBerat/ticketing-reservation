package com.mbi.ticketingreservation.dataintegrationtests;

import com.mbi.ticketingreservation.audit.persistence.AuditLogRepository;
import com.mbi.ticketingreservation.auth.domain.Role;
import com.mbi.ticketingreservation.auth.domain.User;
import com.mbi.ticketingreservation.auth.persistence.UserRepository;
import com.mbi.ticketingreservation.common.error.ApiError;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.idempotency.persistence.IdempotencyKeyRepository;
import com.mbi.ticketingreservation.reservation.domain.Reservation;
import com.mbi.ticketingreservation.reservation.domain.ReservationStatus;
import com.mbi.ticketingreservation.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class OversellingPreventionIntegrationTest {

    private static final Long ORGANIZER_ID = 2L;
    private static final Set<ReservationStatus> ACTIVE_STATUSES = EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    private static final Instant STARTS_AT = Instant.parse("2100-06-01T18:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(2, ChronoUnit.HOURS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanBusinessData() {
        auditLogRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void competingRequestsForFinalSeatReturnOneCreatedAndOneCapacityExceeded() throws Exception {
        Event event = getPublishedEvent(1);
        long eventVersionBeforeReservations = event.getVersion();
        User firstCustomer = createCustomer();
        User secondCustomer = createCustomer();
        CountDownLatch requestsReady = new CountDownLatch(2);
        CountDownLatch startRequests = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(() -> createReservation(
                    event.getId(), firstCustomer.getId(), "00000000-0000-4000-8000-000000000101",
                    requestsReady, startRequests));
            Future<MvcResult> second = executor.submit(() -> createReservation(
                    event.getId(), secondCustomer.getId(), "00000000-0000-4000-8000-000000000102",
                    requestsReady, startRequests));

            assertTrue(requestsReady.await(5, TimeUnit.SECONDS), "Both requests should be ready");
            startRequests.countDown();

            List<MvcResult> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            List<Integer> statuses = results.stream()
                    .map(result -> result.getResponse().getStatus())
                    .sorted()
                    .toList();

            assertEquals(List.of(201, 409), statuses);
            MvcResult rejected = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 409)
                    .findFirst()
                    .orElseThrow();
            assertTrue(rejected.getResponse().getContentAsString()
                    .contains("\"code\":\"" + ApiError.EVENT_CAPACITY_EXCEEDED + "\""));
        }

        assertEquals(1L, reservationRepository.sumSeatsByEventIdAndStatuses(event.getId(), ACTIVE_STATUSES));
        assertEquals(1L, reservationRepository.findAll().stream()
                .filter(reservation -> event.getId().equals(reservation.getEventId()))
                .count());
        assertEquals(eventVersionBeforeReservations,
                eventRepository.findById(event.getId()).orElseThrow().getVersion());
    }

    @Test
    void twentyCompetingRequestsForTenSeatsCreateExactlyTenReservations() throws Exception {
        int requestCount = 20;
        int capacity = 10;
        Event event = getPublishedEvent(capacity);
        List<User> customers = new ArrayList<>(requestCount);
        for (int requestIndex = 0; requestIndex < requestCount; requestIndex++) {
            customers.add(createCustomer());
        }
        CountDownLatch requestsReady = new CountDownLatch(requestCount);
        CountDownLatch startRequests = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            List<Future<MvcResult>> futures = new ArrayList<>(requestCount);
            for (User customer : customers) {
                futures.add(executor.submit(() -> createReservation(
                        event.getId(), customer.getId(), UUID.randomUUID().toString(), requestsReady, startRequests)));
            }

            assertTrue(requestsReady.await(5, TimeUnit.SECONDS), "All requests should be ready");
            startRequests.countDown();

            List<MvcResult> results = new ArrayList<>(requestCount);
            for (Future<MvcResult> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }

            long createdCount = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 201)
                    .count();
            List<MvcResult> rejectedResults = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 409)
                    .toList();

            assertEquals(capacity, createdCount);
            assertEquals(requestCount - capacity, rejectedResults.size());
            for (MvcResult rejected : rejectedResults) {
                assertTrue(rejected.getResponse().getContentAsString()
                        .contains("\"code\":\"" + ApiError.EVENT_CAPACITY_EXCEEDED + "\""));
            }
        }

        assertEquals(capacity,
                reservationRepository.sumSeatsByEventIdAndStatuses(event.getId(), ACTIVE_STATUSES));
        assertEquals(capacity, reservationRepository.findAll().stream()
                .filter(reservation -> event.getId().equals(reservation.getEventId()))
                .count());
    }

    @Test
    void waitingForReservationEventLockTimesOutAfterTTL() throws Exception {
        Event event = getPublishedEvent(1);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> lockHolder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                eventRepository.findByIdForReservation(event.getId()).orElseThrow();
                lockAcquired.countDown();
                await(releaseLock);
            }));

            assertTrue(lockAcquired.await(5, TimeUnit.SECONDS), "First transaction should acquire the event lock");
            long startedAt = System.nanoTime();

            assertThrows(PessimisticLockingFailureException.class,
                    () -> transaction.executeWithoutResult(status ->
                            eventRepository.findByIdForReservation(event.getId()).orElseThrow()));

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            assertTrue(elapsedMillis >= 2_000 && elapsedMillis < 5_000,
                    "Lock wait should time out around three seconds, but took " + elapsedMillis + " ms");

            releaseLock.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
        } finally {
            releaseLock.countDown();
        }
    }

    @Test
    void capacityCannotDropBelowPendingAndConfirmedSeatsButMayEqualThem() throws Exception {
        Event event = getPublishedEvent(10);
        User firstCustomer = createCustomer();
        User secondCustomer = createCustomer();
        reservationRepository.saveAndFlush(new Reservation(event.getId(), firstCustomer.getId(), 2));
        Reservation confirmed = new Reservation(event.getId(), secondCustomer.getId(), 3);
        confirmed.confirm();
        reservationRepository.saveAndFlush(confirmed);

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .with(createOrganizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventUpdateBody(4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiError.EVENT_CAPACITY_BELOW_ACTIVE_RESERVATIONS));

        assertEquals(10, eventRepository.findById(event.getId()).orElseThrow().getCapacity());

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .with(createOrganizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventUpdateBody(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.activeReservedSeats").value(5));
    }

    @Test
    void cancelledSeatsCanBeReservedAgain() throws Exception {
        Event event = getPublishedEvent(2);
        User firstCustomer = createCustomer();
        User secondCustomer = createCustomer();
        Reservation cancelled = new Reservation(event.getId(), firstCustomer.getId(), 2);
        cancelled.cancel();
        reservationRepository.saveAndFlush(cancelled);

        mockMvc.perform(post("/api/events/{eventId}/reservations", event.getId())
                        .with(createCustomerJwt(secondCustomer.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "00000000-0000-4000-8000-000000000103")
                        .content("{\"seats\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats").value(2));

        assertEquals(2L, reservationRepository.sumSeatsByEventIdAndStatuses(event.getId(), ACTIVE_STATUSES));
    }

    private MvcResult createReservation(Long eventId, Long customerId, String idempotencyKey, CountDownLatch requestsReady,
                                        CountDownLatch startRequests) throws Exception {
        requestsReady.countDown();
        if (!startRequests.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent request");
        }
        return mockMvc.perform(post("/api/events/{eventId}/reservations", eventId)
                        .with(createCustomerJwt(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content("{\"seats\":1}"))
                .andReturn();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release event lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding event lock", exception);
        }
    }

    private Event getPublishedEvent(int capacity) {
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

    private RequestPostProcessor createCustomerJwt(Long customerId) {
        return jwt()
                .jwt(token -> token.subject(customerId.toString()).claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private RequestPostProcessor createOrganizerJwt() {
        return jwt()
                .jwt(token -> token.subject(ORGANIZER_ID.toString()).claim("roles", List.of("ORGANIZER")))
                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"));
    }

    private String eventUpdateBody(int capacity) {
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
