package com.whatstheplan.events.integration;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
public class ActuatorTest {

    @Autowired
    private WebTestClient webTestClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    })
    void healthEndpoint_ShouldReturnUp(String endpoint) {
        webTestClient.get()
                .uri(endpoint)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
