package com.mbi.ticketingreservation.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ApiError(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldValidationError> fieldErrors
) {

    public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ADMIN_ROLES_IMMUTABLE = "ADMIN_ROLES_IMMUTABLE";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String EVENT_NOT_FOUND = "EVENT_NOT_FOUND";
    public static final String INVALID_EVENT_STATE = "INVALID_EVENT_STATE";
    public static final String EVENT_CAPACITY_EXCEEDED = "EVENT_CAPACITY_EXCEEDED";
    public static final String ACTIVE_RESERVATION_EXISTS = "ACTIVE_RESERVATION_EXISTS";
    public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    public static final String INVALID_IDEMPOTENCY_KEY = "INVALID_IDEMPOTENCY_KEY";
    public static final String RESERVATION_CONFLICT = "RESERVATION_CONFLICT";
    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    public static final String INVALID_RESERVATION_STATE = "INVALID_RESERVATION_STATE";
    public static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    public ApiError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public record FieldValidationError(String field, String message) {
    }
}
