package com.whatstheplan.events.integration.crud;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCategories;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;

class MyEventsRetrievalControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void whenAMyOrganizedEventsRequest_thenShouldReturnOkEventResponse() {
        // given
        Event event1 = generateEventEntity();
        Event event2 = generateEventEntity();
        List<Category> categories = generateEventCategories();
        eventsRepository.insert(event1).block();
        eventsRepository.insert(event2).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event1.getId(), c.getId())).toList())
                .collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event2.getId(), c.getId())).toList())
                .collectList().block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(2)
                .consumeWith(response -> {
                    assertEventResponse(event1, categories, response.getResponseBody().get(0), event1.getRegistrations());
                    assertEventResponse(event2, categories, response.getResponseBody().get(1), event2.getRegistrations());
                });
    }

    @Test
    void whenAMyOrganizedEventsRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenAMyOrganizedEventsRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isUnauthorized();
    }

}