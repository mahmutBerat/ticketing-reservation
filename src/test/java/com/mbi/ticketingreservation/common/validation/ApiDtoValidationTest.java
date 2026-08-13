package com.mbi.ticketingreservation.common.validation;

import com.mbi.ticketingreservation.auth.api.RegisterRequest;
import com.mbi.ticketingreservation.event.api.CreateEventRequest;
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
                0);

        assertEquals(Set.of("capacity", "dateRangeValid"), violatedProperties(request));
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
