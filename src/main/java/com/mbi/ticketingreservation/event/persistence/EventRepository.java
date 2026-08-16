package com.mbi.ticketingreservation.event.persistence;

import com.mbi.ticketingreservation.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByOrderByCreatedAtDesc();

    List<Event> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

}
