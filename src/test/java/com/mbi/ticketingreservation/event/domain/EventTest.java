package com.mbi.ticketingreservation.event.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private static final Instant STARTS_AT = Instant.parse("2030-06-01T18:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(2, ChronoUnit.HOURS);

    @Test
    void requiresOwner() {
        assertThrows(NullPointerException.class, () ->
                new Event(null, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100));
    }

    @Test
    void updatesDraft() {
        Event event = new Event(2L, null, null, null, null, 10);

        event.update("Updated", "New Hall", STARTS_AT, ENDS_AT, 25);

        assertEquals("Updated", event.getTitle());
        assertEquals("New Hall", event.getVenue());
        assertEquals(STARTS_AT, event.getStartsAt());
        assertEquals(ENDS_AT, event.getEndsAt());
        assertEquals(25, event.getCapacity());
    }

    @Test
    void publishesCompleteDraft() {
        Event event = new Event(2L, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);

        event.publish();

        assertTrue(event.isPublished());
    }

    @Test
    void rejectsPublishingIncompleteDraft() {
        Event event = new Event(2L, null, null, null, null, 100);

        assertThrows(InvalidEventStateException.class, event::publish);
        assertFalse(event.isPublished());
    }

    @Test
    void updatesPublishedEventWhenItRemainsComplete() {
        Event event = new Event(2L, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);
        event.publish();

        event.update("Festival", "Arena", STARTS_AT.plusSeconds(60), ENDS_AT.plusSeconds(60), 200);

        assertTrue(event.isPublished());
        assertEquals("Festival", event.getTitle());
        assertEquals(200, event.getCapacity());
    }

}
