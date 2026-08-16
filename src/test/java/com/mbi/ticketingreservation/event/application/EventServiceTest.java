package com.mbi.ticketingreservation.event.application;

import com.mbi.ticketingreservation.audit.application.AuditService;
import com.mbi.ticketingreservation.event.api.*;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.domain.InvalidEventStateException;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.event.persistence.EventSpecifications;
import com.mbi.ticketingreservation.reservation.application.ReservationReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final Long EVENT_ID = 1000L;
    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "unit-test";
    private static final Instant STARTS_AT = Instant.parse("2030-06-01T18:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2030-06-01T20:00:00Z");
    private Event event;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private EventSpecifications eventSpecifications;

    @Mock
    private ReservationReadService reservationReadService;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                eventSpecifications,
                eventMapper,
                auditService,
                reservationReadService);
        event = event("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);
    }

    @Test
    void createsDraftEvent() {
        CreateEventRequest request = new CreateEventRequest("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);
        EventResponse expectedResponse = response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100, false);

        when(eventMapper.toEntity(request, OWNER_ID)).thenReturn(event);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        EventResponse actualResponse = eventService.create(request, OWNER_ID, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        verify(eventRepository).saveAndFlush(event);
        verify(auditService).saveRecord(OWNER_ID, EventService.EVENT_CREATED, EventService.EVENT_RESOURCE, event.getId(), IP, USER_AGENT);
        verify(eventMapper).toResponse(event);
    }

    @Test
    void updatesEvent() {
        UpdateEventRequest request = new UpdateEventRequest("Festival", "Arena",
                Instant.parse("2030-07-01T18:00:00Z"), Instant.parse("2030-07-01T20:00:00Z"), 200);
        EventResponse expectedResponse = response(
                request.title(), request.venue(), request.startsAt(), request.endsAt(), request.capacity(), false);

        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        EventResponse actualResponse = eventService.update(EVENT_ID, request, OWNER_ID, false, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        assertEquals(request.title(), event.getTitle());
        assertEquals(request.capacity(), event.getCapacity());
        verify(eventRepository).saveAndFlush(event);
        verify(auditService).saveRecord(
                OWNER_ID, EventService.EVENT_UPDATED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);
    }

    @Test
    void allowsCapacityEqualToActiveReservedSeats() {
        UpdateEventRequest request = new UpdateEventRequest(
                "Concert", "Main Hall", STARTS_AT, ENDS_AT, 5);

        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event));
        when(reservationReadService.getActiveSeatsForEvent(EVENT_ID)).thenReturn(5L);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(
                response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 5, false));

        eventService.update(EVENT_ID, request, OWNER_ID, false, IP, USER_AGENT);

        assertEquals(5, event.getCapacity());
    }

    @Test
    void rejectsCapacityBelowActiveReservedSeats() {
        UpdateEventRequest request = new UpdateEventRequest(
                "Concert", "Main Hall", STARTS_AT, ENDS_AT, 4);

        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event));
        when(reservationReadService.getActiveSeatsForEvent(EVENT_ID)).thenReturn(5L);

        assertThrows(
                EventCapacityBelowActiveReservationsException.class,
                () -> eventService.update(EVENT_ID, request, OWNER_ID, false, IP, USER_AGENT));

        assertEquals(100, event.getCapacity());
        verify(eventRepository, never()).saveAndFlush(event);
    }

    @Test
    void publishesEvent() {
        EventResponse expectedResponse = response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100, true);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        EventResponse actualResponse = eventService.publish(EVENT_ID, OWNER_ID, false, IP, USER_AGENT);

        assertEquals(expectedResponse, actualResponse);
        assertEquals(true, event.isPublished());
        verify(eventRepository).saveAndFlush(event);
        verify(auditService).saveRecord(
                OWNER_ID, EventService.EVENT_PUBLISHED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);
    }

    @Test
    void listsOrganizerEvents() {
        EventResponse expectedResponse = response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100, false);

        when(eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        List<EventResponse> actualResponse = eventService.list(null, OWNER_ID, false);

        assertEquals(List.of(expectedResponse), actualResponse);
        verify(eventRepository).findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID);
        verify(eventMapper).toResponse(event);
    }

    @Test
    void listsPublicEvents() {
        PublicEventQuery query = new PublicEventQuery(Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2031-01-01T00:00:00Z"),
                "  concert  ");
        EventResponse expectedResponse = response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100, false);
        Specification<Event> expectedSpecification = (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        Sort expectedSort = Sort.by(Sort.Direction.ASC, "startsAt");

        when(eventSpecifications.getDateAndQueryCriteria(query.from(), query.to(), "concert"))
                .thenReturn(expectedSpecification);
        when(eventRepository.findAll(expectedSpecification, expectedSort)).thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        List<EventResponse> actualResponse = eventService.listPublic(query);

        assertEquals(List.of(expectedResponse), actualResponse);
        verify(eventSpecifications).getDateAndQueryCriteria(query.from(), query.to(), "concert");
        verify(eventRepository).findAll(expectedSpecification, expectedSort);
        verify(eventMapper).toResponse(event);
    }

    @Test
    void getsEventByIdForOwner() {
        EventResponse expectedResponse = response("Concert", "Main Hall", STARTS_AT, ENDS_AT, 100, false);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        EventResponse actualResponse = eventService.getById(EVENT_ID, OWNER_ID, false);

        assertEquals(expectedResponse, actualResponse);
        verify(eventMapper).toResponse(event);
    }

    @Test
    void rejectsGetByIdForAnotherOwner() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.getById(EVENT_ID, 3L, false));

        verifyNoInteractions(eventMapper);
    }

    @Test
    void getsPublishedEventForReservation() {
        Instant reservationStartsAt = Instant.parse("2100-06-01T18:00:00Z");
        Event publishedEvent = event("Concert", "Main Hall", reservationStartsAt,
                Instant.parse("2100-06-01T20:00:00Z"), 100);
        ReflectionTestUtils.setField(publishedEvent, "id", EVENT_ID);
        publishedEvent.publish();
        EventReservationDTO expectedResponse = new EventReservationDTO(EVENT_ID, 100, true, reservationStartsAt);

        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(publishedEvent));

        EventReservationDTO actualResponse = eventService.getForReservation(EVENT_ID);

        assertEquals(expectedResponse, actualResponse);
        verify(eventRepository).findByIdForUpdate(EVENT_ID);
    }

    @Test
    void rejectsDraftEventForReservation() {
        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event));

        assertThrows(
                InvalidEventStateException.class,
                () -> eventService.getForReservation(EVENT_ID));
    }

    private Event event(String title, String venue, Instant startsAt, Instant endsAt, int capacity) {
        return new Event(OWNER_ID, title, venue, startsAt, endsAt, capacity);
    }

    private EventResponse response(String title, String venue, Instant startsAt, Instant endsAt, int capacity, boolean published) {
        return new EventResponse(null, OWNER_ID, title, venue, startsAt, endsAt, capacity, published, 0, null);
    }
}
