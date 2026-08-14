package com.mbi.ticketingreservation.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceResponseHeaderFilterTest {

    @Test
    void exposesCurrentTraceAndSpanIdsAsResponseHeaders() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("0123456789abcdef0123456789abcdef");
        when(context.spanId()).thenReturn("0123456789abcdef");
        TraceResponseHeaderFilter filter = new TraceResponseHeaderFilter(tracer);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (request, servletResponse) -> { });

        assertEquals(
                "0123456789abcdef0123456789abcdef",
                response.getHeader(TraceResponseHeaderFilter.TRACE_ID_HEADER));
        assertEquals("0123456789abcdef", response.getHeader(TraceResponseHeaderFilter.SPAN_ID_HEADER));
    }
}
