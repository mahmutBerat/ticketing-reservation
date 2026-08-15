package com.mbi.ticketingreservation.idempotency.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
@SQLRestriction("deleted_at IS NULL")
// TODO provide uniqueConstraints = @UniqueConstraint(
//                name = "uk_idempotency_keys_scope",
//                columnNames = {"actor_id", "endpoint", "idempotency_key"})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idempotency_key_id_generator")
    @SequenceGenerator(
            name = "idempotency_key_id_generator",
            sequenceName = "idempotency_keys_seq",
            allocationSize = 1)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public IdempotencyKey(Long actorId, String endpoint, String key, String requestHash, Instant expiresAt) {
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.endpoint = requireText(endpoint, "endpoint");
        this.key = requireText(key, "key");
        this.requestHash = requireSha256(requestHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = IdempotencyStatus.PROCESSING;
    }

    public void complete() {
        if (status == IdempotencyStatus.COMPLETED) {
            throw new IllegalStateException("Idempotency record is already completed");
        }
        this.status = IdempotencyStatus.COMPLETED;
    }

    public void softDelete(Instant instant) {
        if (deletedAt != null) {
            throw new IllegalStateException("Idempotency record is already deleted");
        }
        deletedAt = Objects.requireNonNull(instant, "instant must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String requireSha256(String requestHash) {
        String value = requireText(requestHash, "requestHash");
        if (!value.matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException("requestHash must be a SHA-256 hexadecimal value");
        }
        return value.toLowerCase();
    }
}
