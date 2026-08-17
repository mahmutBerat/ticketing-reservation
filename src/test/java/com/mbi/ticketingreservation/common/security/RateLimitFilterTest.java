package com.mbi.ticketingreservation.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsApiRequestsAfterClientReachesLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, clock);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse first = perform(filter, chain, "/api/events", "192.0.2.1");
        MockHttpServletResponse second = perform(filter, chain, "/api/events", "192.0.2.1");
        MockHttpServletResponse rejected = perform(filter, chain, "/api/events", "192.0.2.1");

        assertEquals(200, first.getStatus());
        assertEquals(200, second.getStatus());
        assertEquals(429, rejected.getStatus());
        assertEquals("60", rejected.getHeader("Retry-After"));
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void keepsSeparateLimitsForDifferentClients() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, clock);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse firstClient = perform(filter, chain, "/api/events", "192.0.2.1");
        MockHttpServletResponse secondClient = perform(filter, chain, "/api/events", "192.0.2.2");

        assertEquals(200, firstClient.getStatus());
        assertEquals(200, secondClient.getStatus());
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void allowsRequestsAgainAfterWindowExpires() throws Exception {
        Clock movingClock = mock(Clock.class);
        when(movingClock.millis()).thenReturn(0L, 60_000L);
        RateLimitFilter filter = new RateLimitFilter(1, movingClock);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse first = perform(filter, chain, "/api/events", "192.0.2.1");
        MockHttpServletResponse afterReset = perform(filter, chain, "/api/events", "192.0.2.1");

        assertEquals(200, first.getStatus());
        assertEquals(200, afterReset.getStatus());
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void ignoresNonApiRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, clock);
        FilterChain chain = mock(FilterChain.class);

        perform(filter, chain, "/actuator/health", "192.0.2.1");
        perform(filter, chain, "/actuator/health", "192.0.2.1");

        verify(chain, times(2)).doFilter(any(), any());
    }

    private MockHttpServletResponse perform(RateLimitFilter filter, FilterChain chain, String path, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
