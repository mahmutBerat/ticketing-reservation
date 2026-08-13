package com.mbi.ticketingreservation.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
