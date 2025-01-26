package com.whatstheplan.events.testconfig;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
public class BaseIntegrationTest {

    @BeforeAll
    static void beforeAll() {
    }

}
