package com.mbi.ticketingreservation.event.application;

import com.mbi.ticketingreservation.audit.application.AuditService;
import com.mbi.ticketingreservation.event.api.*;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.event.persistence.EventSpecifications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final Long EVENT_ID = 1000L;
    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "unit-test";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private EventSpecifications eventSpecifications;

    @Mock
    private Specification<Event> specification;

    @Mock
    private Event event;

    @Mock
    private Event savedEvent;

    @InjectMocks
    private EventService eventService;

    @Test
    void createsDraftEventAndRecordsAuditInOrder() {
        CreateEventRequest request = new CreateEventRequest(
                "Concert",
                "Main Hall",
                Instant.parse("2030-06-01T18:00:00Z"),
                Instant.parse("2030-06-01T20:00:00Z"),
                100);
        EventResponse expectedResponse = new EventResponse(
                EVENT_ID,
                OWNER_ID,
                "Concert",
                "Main Hall",
                request.startsAt(),
                request.endsAt(),
                100,
                false,
                0,
                Instant.parse("2030-01-01T12:00:00Z"));
        when(eventMapper.toEntity(request, OWNER_ID)).thenReturn(event);
        when(eventRepository.saveAndFlush(event)).thenReturn(savedEvent);
        when(savedEvent.getId()).thenReturn(EVENT_ID);
        when(eventMapper.toResponse(savedEvent)).thenReturn(expectedResponse);

        EventResponse response = eventService.create(request, OWNER_ID, IP, USER_AGENT);

        assertSame(expectedResponse, response);
        InOrder persistenceOrder = inOrder(eventRepository, auditService);
        persistenceOrder.verify(eventRepository).saveAndFlush(event);
        persistenceOrder.verify(auditService).saveRecord(
                OWNER_ID,
                EventService.EVENT_CREATED,
                EventService.EVENT_RESOURCE,
                EVENT_ID,
                IP,
                USER_AGENT);
        verify(eventMapper).toResponse(savedEvent);
    }

    @Test
    void propagatesAuditFailureWithoutReturningSuccessfulCreateResponse() {
        CreateEventRequest request = createRequest();
        when(eventMapper.toEntity(request, OWNER_ID)).thenReturn(event);
        when(eventRepository.saveAndFlush(event)).thenReturn(savedEvent);
        when(savedEvent.getId()).thenReturn(EVENT_ID);
        doThrow(new IllegalStateException("audit unavailable")).when(auditService).saveRecord(
                OWNER_ID, EventService.EVENT_CREATED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);

        assertThrows(IllegalStateException.class,
                () -> eventService.create(request, OWNER_ID, IP, USER_AGENT));

        verify(eventMapper, never()).toResponse(savedEvent);
    }

    @Test
    void updatesEventAndRecordsAudit() {
        UpdateEventRequest request = updateRequest();
        EventResponse expectedResponse = response("Festival");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.getOwnerId()).thenReturn(OWNER_ID);
        when(eventRepository.saveAndFlush(event)).thenReturn(savedEvent);
        when(eventMapper.toResponse(savedEvent)).thenReturn(expectedResponse);

        EventResponse response = eventService.update(EVENT_ID, request, OWNER_ID, false, IP, USER_AGENT);

        assertSame(expectedResponse, response);
        verify(event).update(
                request.title(), request.venue(), request.startsAt(), request.endsAt(), request.capacity());
        verify(eventRepository).saveAndFlush(event);
        verify(auditService).saveRecord(
                OWNER_ID, EventService.EVENT_UPDATED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);
    }

    @Test
    void rejectsUpdateWhenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> eventService.update(EVENT_ID, updateRequest(), OWNER_ID, false, IP, USER_AGENT));

        verifyNoInteractions(eventMapper, auditService);
    }

    @Test
    void rejectsUpdateForAnotherOwnerWithoutAnotherRepositoryCall() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.getOwnerId()).thenReturn(1L);

        assertThrows(AccessDeniedException.class,
                () -> eventService.update(EVENT_ID, updateRequest(), OWNER_ID, false, IP, USER_AGENT));

        verify(event).getOwnerId();
        verifyNoMoreInteractions(event);
        verify(eventRepository, never()).saveAndFlush(event);
        verifyNoInteractions(eventMapper, auditService);
    }

    @Test
    void adminUpdatesEventWithoutOwnershipRestriction() {
        UpdateEventRequest request = updateRequest();
        EventResponse expectedResponse = response("Festival");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.saveAndFlush(event)).thenReturn(savedEvent);
        when(eventMapper.toResponse(savedEvent)).thenReturn(expectedResponse);

        EventResponse response = eventService.update(EVENT_ID, request, 1L, true, IP, USER_AGENT);

        assertSame(expectedResponse, response);
        verify(event).update(
                request.title(), request.venue(), request.startsAt(), request.endsAt(), request.capacity());
        verify(auditService).saveRecord(
                1L, EventService.EVENT_UPDATED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);
    }

    @Test
    void publishesEventAndRecordsAudit() {
        EventResponse expectedResponse = response("Concert");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.getOwnerId()).thenReturn(OWNER_ID);
        when(eventRepository.saveAndFlush(event)).thenReturn(savedEvent);
        when(eventMapper.toResponse(savedEvent)).thenReturn(expectedResponse);

        EventResponse response = eventService.publish(EVENT_ID, OWNER_ID, false, IP, USER_AGENT);

        assertSame(expectedResponse, response);
        verify(event).publish();
        verify(eventRepository).saveAndFlush(event);
        verify(auditService).saveRecord(
                OWNER_ID, EventService.EVENT_PUBLISHED, EventService.EVENT_RESOURCE, EVENT_ID, IP, USER_AGENT);
    }

    @Test
    void doesNotAuditRejectedPublish() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.getOwnerId()).thenReturn(OWNER_ID);
        doThrow(new IllegalStateException("incomplete")).when(event).publish();

        assertThrows(IllegalStateException.class,
                () -> eventService.publish(EVENT_ID, OWNER_ID, false, IP, USER_AGENT));

        verifyNoInteractions(auditService, eventMapper);
    }

    @Test
    void rejectsPublishForAnotherOwnerWithoutAnotherRepositoryCall() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(event.getOwnerId()).thenReturn(1L);

        assertThrows(AccessDeniedException.class,
                () -> eventService.publish(EVENT_ID, OWNER_ID, false, IP, USER_AGENT));

        verify(event).getOwnerId();
        verifyNoMoreInteractions(event);
        verify(eventRepository, never()).saveAndFlush(event);
        verifyNoInteractions(eventMapper, auditService);
    }

    @Test
    void organizerListsOnlyOwnEventsWhenOwnerFilterIsAbsent() {
        EventResponse expectedResponse = response("Concert");
        when(eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        List<EventResponse> response = eventService.list(null, OWNER_ID, false);

        assertEquals(List.of(expectedResponse), response);
    }

    @Test
    void organizerCannotListAnotherOwnersEvents() {
        assertThrows(AccessDeniedException.class, () -> eventService.list(1L, OWNER_ID, false));

        verifyNoInteractions(eventRepository, eventMapper, auditService);
    }

    @Test
    void adminListsAllEventsWhenOwnerFilterIsAbsent() {
        when(eventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertTrue(eventService.list(null, 1L, true).isEmpty());

        verify(eventRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void adminCanFilterEventsByOwner() {
        when(eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of());

        assertTrue(eventService.list(OWNER_ID, 1L, true).isEmpty());

        verify(eventRepository).findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID);
    }

    @Test
    void listsPublishedEventsWithNormalizedFilters() {
        PublicEventQuery query = new PublicEventQuery(
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2031-01-01T00:00:00Z"),
                "  concert  ");
        EventResponse expectedResponse = response("Concert");
        when(eventSpecifications.getDateAndQueryCriteria(query.from(), query.to(), "concert"))
                .thenReturn(specification);
        when(eventRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "startsAt")))
                .thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expectedResponse);

        List<EventResponse> response = eventService.listPublic(query);

        assertEquals(List.of(expectedResponse), response);
        verify(eventSpecifications).getDateAndQueryCriteria(query.from(), query.to(), "concert");
    }

    @Test
    void treatsBlankPublicSearchAsNoTextFilter() {
        PublicEventQuery query = new PublicEventQuery(null, null, "   ");
        when(eventSpecifications.getDateAndQueryCriteria(null, null, null)).thenReturn(specification);
        when(eventRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "startsAt")))
                .thenReturn(List.of());

        assertTrue(eventService.listPublic(query).isEmpty());
        verify(eventSpecifications).getDateAndQueryCriteria(null, null, null);
    }

    private CreateEventRequest createRequest() {
        return new CreateEventRequest(
                "Concert",
                "Main Hall",
                Instant.parse("2030-06-01T18:00:00Z"),
                Instant.parse("2030-06-01T20:00:00Z"),
                100);
    }

    private UpdateEventRequest updateRequest() {
        return new UpdateEventRequest(
                "Festival",
                "Arena",
                Instant.parse("2030-07-01T18:00:00Z"),
                Instant.parse("2030-07-01T20:00:00Z"),
                200);
    }

    private EventResponse response(String title) {
        return new EventResponse(
                EVENT_ID,
                OWNER_ID,
                title,
                "Main Hall",
                Instant.parse("2030-06-01T18:00:00Z"),
                Instant.parse("2030-06-01T20:00:00Z"),
                100,
                false,
                0,
                Instant.parse("2030-01-01T12:00:00Z"));
    }
}
