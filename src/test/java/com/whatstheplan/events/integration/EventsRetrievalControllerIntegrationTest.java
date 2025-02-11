package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCategories;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static org.assertj.core.api.Assertions.assertThat;

class EventsRetrievalControllerIntegrationTest extends BaseIntegrationTest {

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenANewEventRetrievalRequest_thenShouldReturnOkEventResponse(
            Event event,
            List<Category> categories) {
        // given
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events/" + event.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    assertEventResponse(event, categories, response.getResponseBody().get(0));
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
                Arguments.of(event, categories),
                Arguments.of(event, List.of())
        );
    }

}