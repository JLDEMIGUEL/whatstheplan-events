package com.whatstheplan.events.integration.registration;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.Registration;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static org.assertj.core.api.Assertions.assertThat;

class EventsRegistrationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void whenANewEventRegistrationRequest_thenShouldReturnOkEventResponse() {
        // given
        Event event = generateEventEntity();
        eventsRepository.insert(event).block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events/registration/" + event.getId())
                .exchange()
                .expectStatus().isCreated()
                .expectAll(response -> {
                    Registration registration = registrationRepository.findAll().blockFirst();

                    assertThat(registration.getEventId()).isEqualTo(event.getId());
                    assertThat(registration.getUserId()).isEqualTo(USER_ID);

                    Event savedEvent = eventsRepository.findById(event.getId()).block();
                    assertThat(savedEvent.getRegistrations()).isEqualTo(event.getRegistrations() + 1);
                });
    }

    @Test
    void whenANewEventRegistrationRequestOnAFullEvent_thenShouldReturnBadRequest() {
        // given
        Event event = generateEventEntity();
        event.setRegistrations(event.getCapacity());

        eventsRepository.insert(event).block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events/registration/" + event.getId())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ErrorResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    ErrorResponse errorResponse = response.getResponseBody().get(0);
                    assertThat(errorResponse.getReason())
                            .isEqualTo("The event has reached its maximum capacity.");

                    assertThat(registrationRepository.findAll().hasElements().block()).isFalse();
                });
    }

    @Test
    void whenANewEventRegistrationRequestOnAlreadyRegisteredEvent_thenShouldReturnBadRequest() {
        // given
        Event event = generateEventEntity();

        eventsRepository.insert(event).block();
        registrationRepository.save(Registration.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .eventId(event.getId())
                .isNew(true)
                .build()).block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events/registration/" + event.getId())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ErrorResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    ErrorResponse errorResponse = response.getResponseBody().get(0);
                    assertThat(errorResponse.getReason())
                            .isEqualTo("User already registered");

                    assertThat(registrationRepository.findAll().collectList().block()).hasSize(1);

                    Event savedEvent = eventsRepository.findById(event.getId()).block();
                    assertThat(savedEvent.getRegistrations()).isEqualTo(event.getRegistrations());
                });
    }

    @Test
    void whenANewEventRegistrationRequestWithWrongEventId_thenShouldReturnBadRequest() {
        // given
        UUID wrongEventId = UUID.randomUUID();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events/registration/" + wrongEventId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ErrorResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    ErrorResponse errorResponse = response.getResponseBody().get(0);
                    assertThat(errorResponse.getReason())
                            .isEqualTo("Event not found with id: " + wrongEventId);

                    assertThat(eventsRepository.findAll().hasElements().block()).isFalse();
                });
    }

    @Test
    void whenANewEventRegistrationRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .post()
                .uri("/events/registration/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventRegistrationRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .post()
                .uri("/events/registration/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

}