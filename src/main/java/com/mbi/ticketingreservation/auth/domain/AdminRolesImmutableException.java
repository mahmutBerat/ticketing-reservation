package com.mbi.ticketingreservation.auth.domain;

public class AdminRolesImmutableException extends RuntimeException {

    public AdminRolesImmutableException() {
        super("Roles of an admin user cannot be changed");
    }
}
