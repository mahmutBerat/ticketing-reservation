package com.mbi.ticketingreservation.audit.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditLogTest {

    @Test
    void normalizesOptionalMetadata() {
        AuditLog auditLog = new AuditLog(2L, " EVENT_CREATED ", " EVENT ", 1000L, "  ", null);

        assertEquals("EVENT_CREATED", auditLog.getAction());
        assertEquals("EVENT", auditLog.getResourceType());
        assertNull(auditLog.getIp());
        assertNull(auditLog.getUserAgent());
    }

    @Test
    void truncatesClientControlledMetadataToDatabaseLimits() {
        AuditLog auditLog = new AuditLog(2L, "EVENT_CREATED", "EVENT", 1000L, "i".repeat(46), "u".repeat(513));

        assertEquals(45, auditLog.getIp().length());
        assertEquals(512, auditLog.getUserAgent().length());
    }

    @Test
    void requiresAction() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditLog(2L, " ", "EVENT", 1000L, null, null));
    }

    @Test
    void requiresResourceType() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditLog(2L, "EVENT_CREATED", null, 1000L, null, null));
    }
}
