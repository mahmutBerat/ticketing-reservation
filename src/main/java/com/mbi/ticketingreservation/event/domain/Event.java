package com.mbi.ticketingreservation.event.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_id_generator")
    @SequenceGenerator(name = "event_id_generator", sequenceName = "events_seq", allocationSize = 1)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title")
    private String title;

    @Column(name = "venue")
    private String venue;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "published", nullable = false)
    private boolean published; // draft event flag

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Event(Long ownerId, String title, String venue, Instant startsAt, Instant endsAt, int capacity) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        update(title, venue, startsAt, endsAt, capacity);
    }

    public void update(String title, String venue, Instant startsAt, Instant endsAt, int capacity) {
        if (!isDraft() && !isComplete(title, venue, startsAt, endsAt)) {
            throw new InvalidEventStateException("Published event must remain complete");
        }
        this.title = title;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
    }

    public void publish() {
        if (!isComplete(title, venue, startsAt, endsAt)) {
            throw new InvalidEventStateException("Only a complete event can be published");
        }
        published = true;
    }

    public boolean isDraft() {
        return !published;
    }

    private static boolean isComplete(String title, String venue, Instant startsAt, Instant endsAt) {
        return title != null && venue != null && startsAt != null && endsAt != null;
    }
}
