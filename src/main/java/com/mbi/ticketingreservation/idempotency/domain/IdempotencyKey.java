package com.mbi.ticketingreservation.idempotency.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
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

    public IdempotencyKey(Long actorId, String endpoint, String key, String requestHash, Instant expiresAt) {
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.endpoint = endpoint;
        this.key = key;
        this.requestHash = requestHash;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = IdempotencyStatus.PROCESSING;
    }

    public void complete() {
        if (status == IdempotencyStatus.COMPLETED) {
            throw new IllegalStateException("Idempotency record is already completed");
        }
        this.status = IdempotencyStatus.COMPLETED;
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(Objects.requireNonNull(instant, "instant must not be null"));
    }

}
