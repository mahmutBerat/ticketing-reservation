package com.mbi.ticketingreservation.common.mapping;

import com.mbi.ticketingreservation.auth.api.UserMapper;
import com.mbi.ticketingreservation.auth.domain.Role;
import com.mbi.ticketingreservation.auth.domain.User;
import com.mbi.ticketingreservation.event.api.CreateEventRequest;
import com.mbi.ticketingreservation.event.api.EventMapper;
import com.mbi.ticketingreservation.event.domain.Event;
import com.mbi.ticketingreservation.reservation.api.CreateReservationRequest;
import com.mbi.ticketingreservation.reservation.api.ReservationMapper;
import com.mbi.ticketingreservation.reservation.domain.Reservation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainMapperTest {

    private static final EventMapper EVENT_MAPPER = Mappers.getMapper(EventMapper.class);
    private static final ReservationMapper RESERVATION_MAPPER = Mappers.getMapper(ReservationMapper.class);
    private static final UserMapper USER_MAPPER = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsEventRequestToDomainAndDomainToResponse() {
        CreateEventRequest request = new CreateEventRequest(
                "Concert",
                "Main Hall",
                Instant.parse("2030-06-01T18:00:00Z"),
                Instant.parse("2030-06-01T20:00:00Z"),
                100);

        Event event = EVENT_MAPPER.toEntity(request, 2L);
        var response = EVENT_MAPPER.toResponse(event);

        assertEquals(2L, event.getOwnerId());
        assertEquals("Concert", response.title());
        assertEquals(100, response.capacity());
    }

    @Test
    void mapsReservationRequestToDomainAndDomainToResponse() {
        Reservation reservation = RESERVATION_MAPPER.toEntity(new CreateReservationRequest(2), 10L, 20L);

        var response = RESERVATION_MAPPER.toResponse(reservation);

        assertEquals(10L, response.eventId());
        assertEquals(20L, response.userId());
        assertEquals(2, response.seats());
    }

    @Test
    void mapsUserWithoutExposingPasswordHash() {
        User user = new User("customer@example.com", "secret-hash", Set.of(Role.CUSTOMER));

        var response = USER_MAPPER.toResponse(user);

        assertEquals("customer@example.com", response.email());
        assertEquals(Set.of(Role.CUSTOMER), response.roles());
    }
}
