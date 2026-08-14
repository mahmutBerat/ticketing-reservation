package com.mbi.ticketingreservation.auth.api;

import com.mbi.ticketingreservation.auth.domain.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty Set<@NotNull Role> roles
) {
}
