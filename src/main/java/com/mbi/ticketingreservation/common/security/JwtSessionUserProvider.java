package com.mbi.ticketingreservation.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtSessionUserProvider implements SessionUserProvider {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    @Override
    public SessionUser getSessionUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated JWT principal is available");
        }

        return new SessionUser(
                parseUserId(jwt.getSubject()),
                jwt.getClaimAsString(EMAIL_CLAIM),
                readRoles(jwt));
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Authenticated JWT subject must be a numeric user id", exception);
        }
    }

    private Set<String> readRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        return roles == null ? Set.of() : new LinkedHashSet<>(roles);
    }
}
