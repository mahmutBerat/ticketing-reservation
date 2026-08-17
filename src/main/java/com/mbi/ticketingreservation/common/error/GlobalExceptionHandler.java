package com.mbi.ticketingreservation.common.error;

import com.mbi.ticketingreservation.auth.application.EmailAlreadyRegisteredException;
import com.mbi.ticketingreservation.auth.application.InvalidCredentialsException;
import com.mbi.ticketingreservation.auth.application.InvalidRefreshTokenException;
import com.mbi.ticketingreservation.auth.application.UserNotFoundException;
import com.mbi.ticketingreservation.auth.domain.AdminRolesImmutableException;
import com.mbi.ticketingreservation.common.error.ApiError.FieldValidationError;
import com.mbi.ticketingreservation.event.application.EventCapacityBelowActiveReservationsException;
import com.mbi.ticketingreservation.event.application.EventNotFoundException;
import com.mbi.ticketingreservation.event.domain.InvalidEventStateException;
import com.mbi.ticketingreservation.reservation.application.*;
import com.mbi.ticketingreservation.reservation.domain.InvalidReservationStateException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException exception) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                ApiError.USER_NOT_FOUND,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(AdminRolesImmutableException.class)
    ResponseEntity<ApiError> handleAdminRolesImmutable(AdminRolesImmutableException exception) {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.ADMIN_ROLES_IMMUTABLE,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied() {
        return errorResponse(
                HttpStatus.FORBIDDEN,
                ApiError.ACCESS_DENIED,
                "You are not authorized to perform this operation",
                List.of());
    }

    @ExceptionHandler(EventNotFoundException.class)
    ResponseEntity<ApiError> handleEventNotFound(EventNotFoundException exception) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                ApiError.EVENT_NOT_FOUND,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(InvalidEventStateException.class)
    ResponseEntity<ApiError> handleInvalidEventState(InvalidEventStateException exception) {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.INVALID_EVENT_STATE,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(EventCapacityExceededException.class)
    ResponseEntity<ApiError> handleEventCapacityExceeded(EventCapacityExceededException exception) {
        return errorResponse(HttpStatus.CONFLICT, ApiError.EVENT_CAPACITY_EXCEEDED, exception.getMessage(), List.of());
    }

    @ExceptionHandler(EventCapacityBelowActiveReservationsException.class)
    ResponseEntity<ApiError> handleEventCapacityBelowActiveReservations(
            EventCapacityBelowActiveReservationsException exception) {
        return errorResponse(HttpStatus.CONFLICT, ApiError.EVENT_CAPACITY_BELOW_ACTIVE_RESERVATIONS, exception.getMessage(), List.of());
    }

    @ExceptionHandler(ActiveReservationExistsException.class)
    ResponseEntity<ApiError> handleActiveReservationExists(ActiveReservationExistsException exception) {
        return errorResponse(HttpStatus.CONFLICT, ApiError.ACTIVE_RESERVATION_EXISTS, exception.getMessage(), List.of());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException exception) {
        return errorResponse(HttpStatus.CONFLICT, ApiError.IDEMPOTENCY_KEY_REUSED, exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ResponseEntity<ApiError> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        return errorResponse(HttpStatus.BAD_REQUEST, ApiError.INVALID_IDEMPOTENCY_KEY, exception.getMessage(), List.of());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    ResponseEntity<ApiError> handleReservationNotFound(ReservationNotFoundException exception) {
        return errorResponse(HttpStatus.NOT_FOUND, ApiError.RESERVATION_NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    ResponseEntity<ApiError> handleInvalidReservationState(InvalidReservationStateException exception) {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.INVALID_RESERVATION_STATE,
                exception.getMessage(),
                List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleReservationConflict() {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.RESERVATION_CONFLICT,
                "The reservation or event changed concurrently; retry the request",
                List.of());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleReservationLockConflict() {
        return errorResponse(
                HttpStatus.CONFLICT,
                ApiError.RESERVATION_CONFLICT,
                "The event is busy with another reservation; retry the request",
                List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> handleEndpointNotFound() {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                ApiError.ENDPOINT_NOT_FOUND,
                "API endpoint not found",
                List.of());
    }

    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    ResponseEntity<ApiError> handleInvalidRequest() {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                ApiError.INVALID_REQUEST,
                "Request is invalid or unsupported",
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
        return errorResponse(HttpStatus.BAD_REQUEST, ApiError.MALFORMED_REQUEST,
                "Request body is malformed", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        String traceId = currentTraceId();
        RequestDetails request = currentRequestDetails();
        log.error("Unexpected request failure method={} path={} status={} code={} traceId={}",
                request.method(), request.path(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ApiError.INTERNAL_SERVER_ERROR, traceId, exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(ApiError.INTERNAL_SERVER_ERROR, "An unexpected error occurred", traceId,
                        Instant.now(), List.of()));
    }

    private ResponseEntity<ApiError> errorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldValidationError> fieldErrors
    ) {
        String traceId = currentTraceId();
        RequestDetails request = currentRequestDetails();
        log.warn("Handled request failure method={} path={} status={} code={} traceId={}", request.method(),
                request.path(), status.value(), code, traceId);

        return ResponseEntity.status(status)
                .body(new ApiError(code, message, traceId, Instant.now(), fieldErrors));
    }

    private String currentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan == null ? TRACE_ID_UNAVAILABLE : currentSpan.context().traceId();
    }

    private RequestDetails currentRequestDetails() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return new RequestDetails(request.getMethod(), request.getRequestURI());
        }
        return new RequestDetails("unavailable", "unavailable");
    }

    private record RequestDetails(String method, String path) {
    }
}
