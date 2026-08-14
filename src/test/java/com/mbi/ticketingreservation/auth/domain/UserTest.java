package com.mbi.ticketingreservation.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    void replacesRolesForNonAdminUser() {
        User user = new User("user@example.com", "password-hash", Set.of(Role.CUSTOMER));

        user.replaceRoles(Set.of(Role.ORGANIZER));

        assertEquals(Set.of(Role.ORGANIZER), user.getRoles());
    }

    @Test
    void neverChangesRolesOfExistingAdminUser() {
        User user = new User("admin@example.com", "password-hash", Set.of(Role.ADMIN));

        assertThrows(AdminRolesImmutableException.class,
                () -> user.replaceRoles(Set.of(Role.CUSTOMER)));
        assertEquals(Set.of(Role.ADMIN), user.getRoles());
    }
}
