package com.mbi.ticketingreservation.audit.application;

import com.mbi.ticketingreservation.audit.domain.AuditLog;
import com.mbi.ticketingreservation.audit.persistence.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveRecord(Long actorId, String action, String resourceType, Long resourceId, String ip, String userAgent) {
        auditLogRepository.save(new AuditLog(actorId, action, resourceType, resourceId, ip, userAgent));
    }
}
