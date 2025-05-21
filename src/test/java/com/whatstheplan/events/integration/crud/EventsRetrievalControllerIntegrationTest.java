package com.whatstheplan.events.integration.crud;

import com.whatstheplan.events.client.user.response.BasicUserResponse;
import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.DetailedEventResponse;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.whatstheplan.events.client.user.UserClient.USER_REDIS_KEY;
import static com.whatstheplan.events.services.EventRegistrationService.IS_REGISTERED_KEY;
import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertDetailedEventResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateBasicUserResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCategories;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateRegistration;
import static org.assertj.core.api.Assertions.assertThat;

class EventsRetrievalControllerIntegrationTest extends BaseIntegrationTest {

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenANewEventRetrievalRequest_thenShouldReturnOkEventResponse(
            Event event,
            List<Category> categories,
            boolean isRegistered) {
        // given
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();
        if (isRegistered) {
            registrationRepository.save(generateRegistration(event.getId())).block();
        }

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/" + event.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DetailedEventResponse.class)
                .hasSize(1)
                .consumeWith(response ->
                        assertDetailedEventResponse(event, categories, response.getResponseBody().get(0),
                                event.getRegistrations(), isRegistered, USERNAME));
    }

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenRequestingEvent_thenFirstCallShouldHitServiceAndNextFromCache(
            Event event,
            List<Category> categories,
            boolean isRegistered) {
        // given
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();
        if (isRegistered) {
            registrationRepository.save(generateRegistration(event.getId())).block();
        }

        // when
        IntStream.range(0, 3).forEach(i ->
                webTestClient
                        .mutateWith(JWT)
                        .get()
                        .uri("/events/" + event.getId())
                        .exchange()
                        .expectStatus().isOk());

        // then
        userWireMockExtension.verify(1,
                getRequestedFor(urlEqualTo("/users-info/" + event.getOrganizerId())));
        BasicUserResponse userCache = userReactiveRedisTemplate.opsForValue()
                .get(USER_REDIS_KEY + event.getOrganizerId()).block();
        assertThat(userCache).isEqualTo(generateBasicUserResponse(USERNAME, "email@email.com"));

        Boolean booleanCache = booleanReactiveRedisTemplate.opsForValue()
                .get(IS_REGISTERED_KEY + USER_ID + ":" + event.getId()).block();
        assertThat(booleanCache).isEqualTo(isRegistered);
    }

    @Test
    void whenANewEventRetrievalRequestCreatedByOtherUser_shouldReturnFalseInOwnedField() {
        // given
        Event event = generateEventEntity();
        event.setOrganizerId(OTHER_USER_ID);
        eventsRepository.insert(event).block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/" + event.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DetailedEventResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    assertThat(response.getResponseBody().getFirst().getIsOwnedByUser()).isFalse();
                    assertThat(response.getResponseBody().getFirst().getOrganizerUsername()).isEqualTo(OTHER_USERNAME);
                });
    }

    @Test
    void whenANewEventRetrievalRequestWithWrongEventId_thenShouldReturnBadRequest() {
        // given
        UUID wrongEventId = UUID.randomUUID();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/" + wrongEventId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ErrorResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    ErrorResponse errorResponse = response.getResponseBody().get(0);
                    assertThat(errorResponse.getReason())
                            .isEqualTo("Event not found with id: " + wrongEventId);
                });
    }

    @Test
    void whenANewEventRetrievalRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventRetrievalRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static Stream<Arguments> provideEventEntities() {
        Event event = generateEventEntity();
        List<Category> categories = generateEventCategories();
        return Stream.of(
                Arguments.of(event, categories, true),
                Arguments.of(event, List.of(), true),
                Arguments.of(event, categories, false),
                Arguments.of(event, List.of(), false)
        );
    }

}