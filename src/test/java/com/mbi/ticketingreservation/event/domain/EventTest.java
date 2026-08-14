package com.mbi.ticketingreservation.event.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {

    private static final Instant STARTS_AT = Instant.parse("2030-06-01T18:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(2, ChronoUnit.HOURS);

    @Test
    void requiresOwner() {
        assertThrows(NullPointerException.class, () ->
                new Event(null, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100));
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new Event(2L, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 0));
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () ->
                new Event(2L, "Concert", "Main Hall", ENDS_AT, STARTS_AT, 100));
    }

    @Test
    void rejectsEqualStartAndEnd() {
        assertThrows(IllegalArgumentException.class, () ->
                new Event(2L, "Concert", "Main Hall", STARTS_AT, STARTS_AT, 100));
    }

    @Test
    void createsIncompleteDraftAndNormalizesText() {
        Event event = new Event(2L, "  Concert  ", "   ", null, null, 100);

        assertEquals("Concert", event.getTitle());
        assertNull(event.getVenue());
        assertFalse(event.isPublished());
    }

    @Test
    void updatesDraft() {
        Event event = new Event(2L, null, null, null, null, 10);

        event.update(" Updated ", " New Hall ", STARTS_AT, ENDS_AT, 25);

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

    @ParameterizedTest
    @MethodSource("incompleteEventUpdates")
    void publishedEventCannotBecomeIncomplete(
            String title,
            String venue,
            Instant startsAt,
            Instant endsAt
    ) {
        Event event = new Event(2L, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);
        event.publish();

        assertThrows(InvalidEventStateException.class,
                () -> event.update(title, venue, startsAt, endsAt, 200));
        assertEquals("Concert", event.getTitle());
        assertEquals(100, event.getCapacity());
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

    @Test
    void invalidUpdateDoesNotPartiallyMutateEvent() {
        Event event = new Event(2L, "Concert", "Main Hall", STARTS_AT, ENDS_AT, 100);

        assertThrows(IllegalArgumentException.class,
                () -> event.update("Changed", "Arena", ENDS_AT, STARTS_AT, 200));

        assertEquals("Concert", event.getTitle());
        assertEquals(100, event.getCapacity());
    }

    private static Stream<Arguments> incompleteEventUpdates() {
        return Stream.of(
                Arguments.of(null, "Main Hall", STARTS_AT, ENDS_AT),
                Arguments.of("Concert", " ", STARTS_AT, ENDS_AT),
                Arguments.of("Concert", "Main Hall", null, ENDS_AT),
                Arguments.of("Concert", "Main Hall", STARTS_AT, null));
    }
}
