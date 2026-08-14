package com.mbi.ticketingreservation.audit.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends BaseEntity {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_id_generator")
    @SequenceGenerator(name = "audit_log_id_generator", sequenceName = "audit_logs_seq", allocationSize = 50)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    public AuditLog(
            Long actorId,
            String action,
            String resourceType,
            Long resourceId,
            String ip,
            String userAgent
    ) {
        this.actorId = actorId;
        this.action = requireText(action, "action");
        this.resourceType = requireText(resourceType, "resourceType");
        this.resourceId = resourceId;
        this.ip = trim(trimToNull(ip), MAX_IP_LENGTH);
        this.userAgent = trim(trimToNull(userAgent), MAX_USER_AGENT_LENGTH);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trim(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
