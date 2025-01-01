package com.whatstheplan.events.integration;

import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

class EventsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testEndpoint_ShouldReturnUp() {
        webTestClient.get()
                .uri("/events/test")
                .exchange()
                .expectStatus().isOk();
    }
}