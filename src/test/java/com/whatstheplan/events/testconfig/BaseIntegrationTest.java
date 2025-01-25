package com.whatstheplan.events.testconfig;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;

@WebFluxTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
public class BaseIntegrationTest {

    @BeforeAll
    static void beforeAll() {
    }

}
