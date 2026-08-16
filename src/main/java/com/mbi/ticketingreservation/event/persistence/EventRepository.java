package com.mbi.ticketingreservation.event.persistence;

import com.mbi.ticketingreservation.event.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByOrderByCreatedAtDesc();

    List<Event> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select event from Event event where event.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") Long eventId);

}
