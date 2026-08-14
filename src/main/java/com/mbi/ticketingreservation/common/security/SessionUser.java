package com.mbi.ticketingreservation.common.security;

import java.util.Objects;
import java.util.Set;

public record SessionUser(Long userId, String email, Set<String> roles) {

    public SessionUser(Long userId, String email, Set<String> roles) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.email = email;
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
