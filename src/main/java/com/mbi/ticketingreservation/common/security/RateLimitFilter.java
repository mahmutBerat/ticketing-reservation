package com.mbi.ticketingreservation.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final Clock clock;

    public RateLimitFilter(@Value("${rate-limit.requests-per-minute:60}") int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = requestsPerMinute;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long now = clock.millis();
        RequestWindow requestWindow = recordRequest(request.getRemoteAddr(), now);

        if (requestWindow.requestCount() > requestsPerMinute) {
            long retryAfterSeconds = Math.max(1, (requestWindow.endsAt() - now + 999) / 1_000);
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RequestWindow recordRequest(String client, long now) {
        return windows.compute(client, (key, current) -> {
            if (current == null || now >= current.endsAt()) {
                return new RequestWindow(1, now + WINDOW_MILLIS);
            }
            return new RequestWindow(current.requestCount() + 1, current.endsAt());
        });
    }

    private record RequestWindow(int requestCount, long endsAt) {
    }
}
