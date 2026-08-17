package com.mbi.ticketingreservation.dataintegrationtests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = "rate-limit.requests-per-minute=2")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitResponseIsNotReplacedByUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
