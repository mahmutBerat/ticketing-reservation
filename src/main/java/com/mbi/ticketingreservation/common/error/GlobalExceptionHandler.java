package com.mbi.ticketingreservation.common.error;

import com.mbi.ticketingreservation.auth.application.EmailAlreadyRegisteredException;
import com.mbi.ticketingreservation.auth.application.InvalidCredentialsException;
import com.mbi.ticketingreservation.auth.application.InvalidRefreshTokenException;
import com.mbi.ticketingreservation.common.error.ApiError.FieldValidationError;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final String TRACE_ID_UNAVAILABLE = "unavailable";

    private final Tracer tracer;

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.EMAIL_ALREADY_REGISTERED,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiError.INVALID_CREDENTIALS,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiError.INVALID_REFRESH_TOKEN,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new FieldValidationError(
                        error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
                .toList();

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                ApiError.VALIDATION_FAILED,
                "Request validation failed",
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedRequest() {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                ApiError.MALFORMED_REQUEST,
                "Request body is malformed",
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        String traceId = currentTraceId();
        log.error("Unexpected request failure traceId={}", traceId, exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        ApiError.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        traceId,
                        Instant.now(),
                        List.of()));
    }

    private ResponseEntity<ApiError> errorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldValidationError> fieldErrors
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, currentTraceId(), Instant.now(), fieldErrors));
    }

    private String currentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan == null ? TRACE_ID_UNAVAILABLE : currentSpan.context().traceId();
    }
}
