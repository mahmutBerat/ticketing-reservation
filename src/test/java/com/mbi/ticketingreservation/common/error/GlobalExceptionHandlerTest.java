package com.mbi.ticketingreservation.common.error;

import com.mbi.ticketingreservation.auth.application.EmailAlreadyRegisteredException;
import com.mbi.ticketingreservation.auth.application.InvalidCredentialsException;
import com.mbi.ticketingreservation.auth.application.InvalidRefreshTokenException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private Tracer tracer;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("0123456789abcdef0123456789abcdef");

        exceptionHandler = new GlobalExceptionHandler(tracer);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void returnsFieldErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiError.VALIDATION_FAILED))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    @Test
    void returnsStandardErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiError.MALFORMED_REQUEST))
                .andExpect(jsonPath("$.message").value("Request body is malformed"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void mapsEmailAlreadyRegisteredToConflict() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleEmailAlreadyRegistered(new EmailAlreadyRegisteredException());

        assertError(response, HttpStatus.CONFLICT, ApiError.EMAIL_ALREADY_REGISTERED,
                "Email is already registered");
    }

    @Test
    void mapsInvalidCredentialsToUnauthorized() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleInvalidCredentials(new InvalidCredentialsException());

        assertError(response, HttpStatus.UNAUTHORIZED, ApiError.INVALID_CREDENTIALS,
                "Email or password is incorrect");
    }

    @Test
    void mapsInvalidRefreshTokenToUnauthorized() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleInvalidRefreshToken(new InvalidRefreshTokenException());

        assertError(response, HttpStatus.UNAUTHORIZED, ApiError.INVALID_REFRESH_TOKEN,
                "Refresh token is invalid or expired");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleUnexpected(new IllegalStateException("sensitive internal detail"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, ApiError.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    @Test
    void usesUnavailableWhenNoTraceIsActive() {
        when(tracer.currentSpan()).thenReturn(null);

        ResponseEntity<ApiError> response = exceptionHandler.handleMalformedRequest();

        assertNotNull(response.getBody());
        assertEquals("unavailable", response.getBody().traceId());
    }

    private void assertError(
            ResponseEntity<ApiError> response,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage
    ) {
        ApiError error = response.getBody();
        assertNotNull(error);
        assertEquals(expectedStatus, response.getStatusCode());
        assertEquals(expectedCode, error.code());
        assertEquals(expectedMessage, error.message());
        assertEquals("0123456789abcdef0123456789abcdef", error.traceId());
    }

    @RestController
    private static class TestController {

        @PostMapping("/test")
        void validate(@Valid @RequestBody TestRequest request) {
        }
    }

    private record TestRequest(@NotBlank String name) {
    }
}
