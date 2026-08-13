package com.mbi.ticketingreservation.audit.persistence;

import com.mbi.ticketingreservation.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
