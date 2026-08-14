package com.mbi.ticketingreservation.event.persistence;

import com.mbi.ticketingreservation.event.domain.Event;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EventSpecifications {

    public Specification<Event> getDateAndQueryCriteria(Instant from, Instant to, String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("published")));
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startsAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startsAt"), to));
            }
            if (query != null) {
                String pattern = "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("venue")), pattern, '\\')));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
