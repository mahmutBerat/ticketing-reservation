package com.mbi.ticketingreservation.common.validation;

import com.mbi.ticketingreservation.auth.api.RegisterRequest;
import com.mbi.ticketingreservation.event.api.CreateEventRequest;
import com.mbi.ticketingreservation.event.api.PublicEventQuery;
import com.mbi.ticketingreservation.event.api.UpdateEventRequest;
import com.mbi.ticketingreservation.reservation.api.CreateReservationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validatesRegistrationInput() {
        RegisterRequest request = new RegisterRequest("invalid-email", "short");

        assertEquals(Set.of("email", "password"), violatedProperties(request));
    }

    @Test
    void validatesEventCapacityAndDateRange() {
        CreateEventRequest request = new CreateEventRequest(
                "Concert",
                "Main Hall",
                Instant.parse("2030-06-01T20:00:00Z"),
                Instant.parse("2030-06-01T18:00:00Z"),
                10);

        assertEquals(Set.of("dateRangeValid"), violatedProperties(request));
    }

    @Test
    void acceptsCreateEventWithAnOpenOrOrderedDateRange() {
        Instant startsAt = Instant.parse("2030-06-01T18:00:00Z");
        Instant endsAt = Instant.parse("2030-06-01T20:00:00Z");

        assertEquals(Set.of(), violatedProperties(
                new CreateEventRequest("Concert", "Main Hall", null, endsAt, 10)));
        assertEquals(Set.of(), violatedProperties(
                new CreateEventRequest("Concert", "Main Hall", startsAt, null, 10)));
        assertEquals(Set.of(), violatedProperties(
                new CreateEventRequest("Concert", "Main Hall", startsAt, endsAt, 10)));
    }

    @Test
    void validatesEventUpdateCapacityAndDateRange() {
        UpdateEventRequest request = new UpdateEventRequest(
                "Concert",
                "Main Hall",
                Instant.parse("2030-06-01T20:00:00Z"),
                Instant.parse("2030-06-01T18:00:00Z"),
                0);

        assertEquals(Set.of("capacity", "dateRangeValid"), violatedProperties(request));
    }

    @Test
    void acceptsEventUpdateWithAnOpenDateRange() {
        Instant startsAt = Instant.parse("2030-06-01T18:00:00Z");
        Instant endsAt = Instant.parse("2030-06-01T20:00:00Z");

        assertEquals(Set.of(), violatedProperties(
                new UpdateEventRequest("Concert", "Main Hall", null, endsAt, 10)));
        assertEquals(Set.of(), violatedProperties(
                new UpdateEventRequest("Concert", "Main Hall", startsAt, null, 10)));
    }

    @Test
    void validatesPublicEventQueryDateRangeAndTextLength() {
        PublicEventQuery query = new PublicEventQuery(
                Instant.parse("2030-06-01T20:00:00Z"),
                Instant.parse("2030-06-01T18:00:00Z"),
                "q".repeat(256));

        assertEquals(Set.of("dateRangeValid", "q"), violatedProperties(query));
    }

    @Test
    void acceptsPublicEventQueryWithAnOpenOrEqualDateRange() {
        Instant from = Instant.parse("2030-06-01T18:00:00Z");
        Instant to = Instant.parse("2030-06-01T20:00:00Z");

        assertEquals(Set.of(), violatedProperties(new PublicEventQuery(null, to, null)));
        assertEquals(Set.of(), violatedProperties(new PublicEventQuery(from, null, null)));
        assertEquals(Set.of(), violatedProperties(new PublicEventQuery(from, from, null)));
    }

    @Test
    void validatesReservationSeatCount() {
        CreateReservationRequest request = new CreateReservationRequest(null);

        assertEquals(Set.of("seats"), violatedProperties(request));
    }

    private Set<String> violatedProperties(Object value) {
        return validator.validate(value).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
