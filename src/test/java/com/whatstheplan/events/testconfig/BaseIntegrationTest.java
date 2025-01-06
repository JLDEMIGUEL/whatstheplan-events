package com.whatstheplan.events.testconfig;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WebFluxTest
public class BaseIntegrationTest {

    @BeforeAll
    static void beforeAll() {
    }

}
