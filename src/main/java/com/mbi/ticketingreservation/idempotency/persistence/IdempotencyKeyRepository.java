package com.mbi.ticketingreservation.idempotency.persistence;

import com.mbi.ticketingreservation.idempotency.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByActorIdAndEndpointAndKey(Long actorId, String endpoint, String key);
}
