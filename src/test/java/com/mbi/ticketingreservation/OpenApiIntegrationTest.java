package com.mbi.ticketingreservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesEssentialOpenApiContract() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(content);

        assertEquals("Ticketing Reservation API", document.at("/info/title").asText());
        assertEquals("http", document.at("/components/securitySchemes/bearerAuth/type").asText());
        assertEquals("bearer", document.at("/components/securitySchemes/bearerAuth/scheme").asText());

        JsonNode createReservation = document.at("/paths/~1api~1events~1{eventId}~1reservations/post");
        assertTrue(createReservation.path("security").toString().contains("bearerAuth"));
        assertTrue(StreamSupport.stream(createReservation.path("parameters").spliterator(), false)
                .anyMatch(parameter -> "Idempotency-Key".equals(parameter.path("name").asText())
                        && parameter.path("required").asBoolean()));
        assertTrue(createReservation.path("responses").has("409"));

        assertEquals(2, document.at("/components/schemas/CreateReservationRequest/properties/seats/example").asInt());
        assertEquals("PENDING",
                document.at("/components/schemas/ReservationResponse/properties/status/example").asText());

        JsonNode login = document.at("/paths/~1api~1auth~1login/post");
        assertFalse(login.has("security"), "Login must remain public");
    }
}
