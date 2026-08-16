package com.mbi.ticketingreservation.event.application;

import com.mbi.ticketingreservation.audit.application.AuditService;
import com.mbi.ticketingreservation.event.api.*;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.event.domain.InvalidEventStateException;
import com.mbi.ticketingreservation.event.persistence.EventRepository;
import com.mbi.ticketingreservation.event.persistence.EventSpecifications;
import com.mbi.ticketingreservation.reservation.application.ReservationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    static final String EVENT_CREATED = "EVENT_CREATED";
    static final String EVENT_UPDATED = "EVENT_UPDATED";
    static final String EVENT_PUBLISHED = "EVENT_PUBLISHED";
    static final String EVENT_RESOURCE = "EVENT";

    private final EventRepository eventRepository;
    private final EventSpecifications eventSpecifications;
    private final EventMapper eventMapper;
    private final AuditService auditService;
    private final ReservationReadService reservationReadService;

    @Transactional
    public EventResponse create(CreateEventRequest request, Long ownerId, String ip, String userAgent) {
        Event event = eventMapper.toEntity(request, ownerId);
        Event savedEvent = eventRepository.saveAndFlush(event);
        auditService.saveRecord(ownerId, EVENT_CREATED, EVENT_RESOURCE, savedEvent.getId(), ip, userAgent);
        return eventMapper.toResponse(savedEvent);
    }

    @Transactional
    public EventResponse update(Long eventId, UpdateEventRequest request, Long ownerId, boolean admin, String ip, String userAgent) {
        Event event = getEventByIdForUpdate(eventId);
        verifyCanModify(event, ownerId, admin);
        long activeSeats = reservationReadService.getActiveSeatsForEvent(eventId);
        if (request.capacity() < activeSeats) {
            throw new EventCapacityBelowActiveReservationsException();
        }
        event.update(request.title(), request.venue(), request.startsAt(), request.endsAt(), request.capacity());
        Event savedEvent = eventRepository.saveAndFlush(event);
        auditService.saveRecord(ownerId, EVENT_UPDATED, EVENT_RESOURCE, eventId, ip, userAgent);
        return eventMapper.toResponse(savedEvent);
    }

    @Transactional
    public EventResponse publish(Long eventId, Long ownerId, boolean admin, String ip, String userAgent) {
        Event event = getEventById(eventId);
        verifyCanModify(event, ownerId, admin);
        event.publish();
        Event savedEvent = eventRepository.saveAndFlush(event);
        auditService.saveRecord(ownerId, EVENT_PUBLISHED, EVENT_RESOURCE, eventId, ip, userAgent);
        return eventMapper.toResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(Long requestedOwnerId, Long ownerId, boolean admin) {
        if (!admin && requestedOwnerId != null && !requestedOwnerId.equals(ownerId)) {
            throw new AccessDeniedException("Organizer cannot list another owner's events");
        }
        Long effectiveOwnerId = admin ? requestedOwnerId : ownerId;
        List<Event> events = effectiveOwnerId == null
                ? eventRepository.findAllByOrderByCreatedAtDesc()
                : eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(effectiveOwnerId);
        return events.stream().map(eventMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listPublic(PublicEventQuery query) {
        String normalizedQuery = query.q() == null || query.q().isBlank() ? null : query.q().trim();
        return eventRepository.findAll(
                        eventSpecifications.getDateAndQueryCriteria(query.from(), query.to(), normalizedQuery),
                        Sort.by(Sort.Direction.ASC, "startsAt"))
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
    }

    private Event getEventByIdForUpdate(Long eventId) {
        return eventRepository.findByIdForUpdate(eventId).orElseThrow(EventNotFoundException::new);
    }

    private void verifyCanModify(Event event, Long ownerId, boolean admin) {
        if (!admin && !event.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("Organizer cannot modify another owner's event");
        }
    }

    public EventResponse getById(Long id, Long ownerId, boolean admin) {
        Event eventById = getEventById(id);
        verifyCanModify(eventById, ownerId, admin);
        return eventMapper.toResponse(eventById);
    }

    @Transactional
    public EventReservationDTO getForReservation(Long eventId) {
        Event event = getEventByIdForUpdate(eventId);
        if (event.isDraft()) {
            throw new InvalidEventStateException("Reservations require a published event");
        }
        if (event.getStartsAt() == null || !event.getStartsAt().isAfter(Instant.now())) {
            throw new InvalidEventStateException("Reservations close when the event starts");
        }
        return new EventReservationDTO(event.getId(), event.getCapacity(), event.isPublished(), event.getStartsAt());
    }
}
