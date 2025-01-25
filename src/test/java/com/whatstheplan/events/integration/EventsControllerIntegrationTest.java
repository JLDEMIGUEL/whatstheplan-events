package com.whatstheplan.events.integration;

import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

class EventsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    public static final SecurityMockServerConfigurers.JwtMutator JWT = mockJwt().jwt(jwt -> jwt
            .claim("cognito:groups", List.of("user")));

    @Test
    void testEndpoint_ShouldReturnUp() {
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/test")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testEndpointWithoutToken_ShouldReturnUnauthorized() {
        webTestClient
                .get()
                .uri("/events/test")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}