package com.whatstheplan.events.testconfig;

import com.whatstheplan.events.repository.EventsCategoriesRepository;
import com.whatstheplan.events.repository.EventsRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class BaseIntegrationTest {

    public static final UUID USER_ID = UUID.randomUUID();

    protected static final SecurityMockServerConfigurers.JwtMutator JWT = mockJwt().jwt(jwt -> jwt
                    .claim("sub", USER_ID)
                    .claim("cognito:groups", List.of("user")))
            .authorities(new SimpleGrantedAuthority("ROLE_user"));

    protected static final SecurityMockServerConfigurers.JwtMutator JWT_NO_ROLE = mockJwt().jwt(jwt -> jwt
            .claim("sub", USER_ID));

    @MockitoSpyBean
    protected EventsRepository eventsRepository;

    @MockitoSpyBean
    protected EventsCategoriesRepository eventsCategoriesRepository;

    @Autowired
    protected WebTestClient webTestClient;

    private static EmbeddedPostgres pg;


    @BeforeAll
    static void beforeAll() {
        try {
            pg = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start embedded PostgreSQL", e);
        }
    }

    @BeforeEach
    void beforeEach() {
        eventsRepository.deleteAll().block();
        eventsCategoriesRepository.deleteAll().block();
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (pg != null) {
            pg.close();
        }
    }

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = pg.getJdbcUrl("postgres", "postgres");
        String r2dbcUrl = "r2dbc:" + jdbcUrl.replace("jdbc:", "").split("\\?")[0];

        registry.add("spring.r2dbc.url", () -> r2dbcUrl + "?currentSchema=events");
        registry.add("spring.r2dbc.username", () -> "postgres");
        registry.add("spring.r2dbc.password", () -> "postgres");
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.username", () -> "postgres");
        registry.add("spring.flyway.password", () -> "postgres");
    }
}
