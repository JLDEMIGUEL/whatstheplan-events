package com.whatstheplan.events.integration.registration;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.Registration;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.testconfig.utils.DataMockUtils.TODAY;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static org.assertj.core.api.Assertions.assertThat;

class MyEventsRegistrationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void whenAMyRegisteredEventsRequestOnAlreadyRegisteredEvent_thenShouldReturnBadRequest() {
        // given
        Event event1 = generateEventEntity();
        Event event2 = generateEventEntity().toBuilder().dateTime(TODAY.plusDays(1).withNano(0)).build();

        eventsRepository.insert(event1).block();
        registrationRepository.save(Registration.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .eventId(event1.getId())
                .isNew(true)
                .build()).block();

        eventsRepository.insert(event2).block();
        registrationRepository.save(Registration.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .eventId(event2.getId())
                .isNew(true)
                .build()).block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/registration")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(2)
                .consumeWith(response -> {
                    List<EventResponse> responses = response.getResponseBody();
                    assertThat(responses).hasSize(2);
                    assertThat(responses)
                            .extracting(EventResponse::getDateTime)
                            .isSorted();
                });
    }

    @Test
    void whenAMyRegisteredEventsRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .get()
                .uri("/events/registration")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenAMyRegisteredEventsRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .get()
                .uri("/events/registration")
                .exchange()
                .expectStatus().isUnauthorized();
    }

}