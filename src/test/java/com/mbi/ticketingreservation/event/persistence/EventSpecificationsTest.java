package com.mbi.ticketingreservation.event.persistence;

import com.mbi.ticketingreservation.event.domain.Event;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSpecificationsTest {

    @Mock
    private Root<Event> root;

    @Mock
    private CriteriaQuery<?> criteriaQuery;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<Boolean> publishedPath;

    @Mock
    private Path<Instant> startsAtPath;

    @Mock
    private Path<String> titlePath;

    @Mock
    private Path<String> venuePath;

    @Mock
    private Expression<String> lowerTitle;

    @Mock
    private Expression<String> lowerVenue;

    @Mock
    private Predicate publishedPredicate;

    @Mock
    private Predicate fromPredicate;

    @Mock
    private Predicate toPredicate;

    @Mock
    private Predicate titlePredicate;

    @Mock
    private Predicate venuePredicate;

    @Mock
    private Predicate queryPredicate;

    @Mock
    private Predicate combinedPredicate;

    private EventSpecifications eventSpecifications;

    @BeforeEach
    void setUp() {
        eventSpecifications = new EventSpecifications();
        when(root.<Boolean>get("published")).thenReturn(publishedPath);
        when(criteriaBuilder.isTrue(publishedPath)).thenReturn(publishedPredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(combinedPredicate);
    }

    @Test
    void createsPublishedOnlyPredicateWhenOptionalFiltersAreAbsent() {
        Specification<Event> specification = eventSpecifications.getDateAndQueryCriteria(null, null, null);

        Predicate result = specification.toPredicate(root, criteriaQuery, criteriaBuilder);

        assertSame(combinedPredicate, result);
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(criteriaBuilder).and(predicates.capture());
        assertEquals(1, predicates.getValue().length);
        assertSame(publishedPredicate, predicates.getValue()[0]);
        verify(root, never()).get("startsAt");
        verify(root, never()).get("title");
        verify(root, never()).get("venue");
    }

    @Test
    void createsDateAndEscapedCaseInsensitiveTextPredicates() {
        Instant from = Instant.parse("2030-01-01T00:00:00Z");
        Instant to = Instant.parse("2031-01-01T00:00:00Z");
        String expectedPattern = "%50\\%\\_off\\\\sale%";
        when(root.<Instant>get("startsAt")).thenReturn(startsAtPath);
        when(criteriaBuilder.greaterThanOrEqualTo(startsAtPath, from)).thenReturn(fromPredicate);
        when(criteriaBuilder.lessThanOrEqualTo(startsAtPath, to)).thenReturn(toPredicate);
        when(root.<String>get("title")).thenReturn(titlePath);
        when(root.<String>get("venue")).thenReturn(venuePath);
        when(criteriaBuilder.lower(titlePath)).thenReturn(lowerTitle);
        when(criteriaBuilder.lower(venuePath)).thenReturn(lowerVenue);
        when(criteriaBuilder.like(lowerTitle, expectedPattern, '\\')).thenReturn(titlePredicate);
        when(criteriaBuilder.like(lowerVenue, expectedPattern, '\\')).thenReturn(venuePredicate);
        when(criteriaBuilder.or(titlePredicate, venuePredicate)).thenReturn(queryPredicate);
        Specification<Event> specification = eventSpecifications.getDateAndQueryCriteria(
                from,
                to,
                "50%_OFF\\Sale");

        Predicate result = specification.toPredicate(root, criteriaQuery, criteriaBuilder);

        assertSame(combinedPredicate, result);
        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(criteriaBuilder).and(predicates.capture());
        assertEquals(4, predicates.getValue().length);
        assertSame(publishedPredicate, predicates.getValue()[0]);
        assertSame(fromPredicate, predicates.getValue()[1]);
        assertSame(toPredicate, predicates.getValue()[2]);
        assertSame(queryPredicate, predicates.getValue()[3]);
        verify(criteriaBuilder).like(lowerTitle, expectedPattern, '\\');
        verify(criteriaBuilder).like(lowerVenue, expectedPattern, '\\');
    }
}
