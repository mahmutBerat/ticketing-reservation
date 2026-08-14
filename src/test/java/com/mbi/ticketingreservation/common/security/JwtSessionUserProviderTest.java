package com.mbi.ticketingreservation.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtSessionUserProviderTest {

    private final JwtSessionUserProvider provider = new JwtSessionUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsAuthenticatedUserFromJwtPrincipal() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("42")
                .claim("email", "admin@example.com")
                .claim("roles", List.of("ORGANIZER", "ADMIN"))
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        SessionUser sessionUser = provider.getSessionUser();

        assertEquals(42L, sessionUser.userId());
        assertEquals("admin@example.com", sessionUser.email());
        assertEquals(Set.of("ORGANIZER", "ADMIN"), sessionUser.roles());
        assertTrue(sessionUser.isAdmin());
        assertTrue(sessionUser.hasRole("ORGANIZER"));
        assertFalse(sessionUser.hasRole("CUSTOMER"));
        assertThrows(UnsupportedOperationException.class, () -> sessionUser.roles().add("CUSTOMER"));
    }

    @Test
    void rejectsMissingAuthentication() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, provider::getSessionUser);

        assertEquals("No authenticated JWT principal is available", exception.getMessage());
    }

    @Test
    void rejectsNonJwtPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password"));

        assertThrows(IllegalStateException.class, provider::getSessionUser);
    }

    @Test
    void rejectsNonNumericSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("not-a-number")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, provider::getSessionUser);

        assertEquals("Authenticated JWT subject must be a numeric user id", exception.getMessage());
    }
}
