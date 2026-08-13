package com.mbi.ticketingreservation.auth.api;

import com.mbi.ticketingreservation.auth.domain.Role;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        Set<Role> roles,
        Instant createdAt,
        Instant lastLoginAt
) {
}
