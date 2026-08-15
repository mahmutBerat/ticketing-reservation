package com.mbi.ticketingreservation.audit.application;

import com.mbi.ticketingreservation.audit.domain.AuditLog;
import com.mbi.ticketingreservation.audit.persistence.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void savesAuditRecord() {
        auditService.saveRecord(2L, "EVENT_UPDATED", "EVENT", 1000L, "127.0.0.1", "unit-test");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertEquals(2L, auditLog.getActorId());
        assertEquals("EVENT_UPDATED", auditLog.getAction());
        assertEquals("EVENT", auditLog.getResourceType());
        assertEquals(1000L, auditLog.getResourceId());
        assertEquals("127.0.0.1", auditLog.getIp());
        assertEquals("unit-test", auditLog.getUserAgent());
    }

    @Test
    void propagatesRepositoryFailure() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class,
                () -> auditService.saveRecord(2L, "EVENT_UPDATED", "EVENT", 1000L, null, null));
    }
}
