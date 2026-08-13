package com.mbi.ticketingreservation.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TraceResponseHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            response.setHeader(TRACE_ID_HEADER, currentSpan.context().traceId());
            response.setHeader(SPAN_ID_HEADER, currentSpan.context().spanId());
        }
        filterChain.doFilter(request, response);
    }
}
