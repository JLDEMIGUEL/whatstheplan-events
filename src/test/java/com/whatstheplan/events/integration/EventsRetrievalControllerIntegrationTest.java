package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.whatstheplan.events.utils.RecurrenceUtils.parseRRule;
import static com.whatstheplan.events.utils.RecurrenceUtilsTest.assertRecurrenceEquals;
import static org.assertj.core.api.Assertions.assertThat;

class EventsRetrievalControllerIntegrationTest extends BaseIntegrationTest {

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenANewEventRetrievalRequest_thenShouldReturnOkEventResponse(
            Event event,
            List<EventCategories> eventCategories) {
        // given
        eventsRepository.insert(event).block();
        eventsCategoriesRepository.saveAll(eventCategories).collectList().block();

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
                    assertEventResponse(event, eventCategories, response.getResponseBody());
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
        List<EventCategories> eventCategories = generateEventCategories(event.getId());
        return Stream.of(
                Arguments.of(event, eventCategories),
                Arguments.of(event, List.of())
        );
    }

    private static void assertEventResponse(Event eventEntity, List<EventCategories> eventCategories,
                                            List<EventResponse> events) {
        assertThat(events).isNotNull();
        assertThat(events).hasSize(1);

        EventResponse eventResponse = events.get(0);
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(eventEntity.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(eventEntity.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(eventEntity.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(eventEntity.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(eventEntity.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(eventEntity.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(eventEntity.getImageKey());
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(eventResponse.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(eventResponse.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();

        assertThat(eventResponse.getActivityTypes())
                .containsAll(eventCategories.stream().map(EventCategories::getActivityType).toList());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(eventCategories.size());

        assertRecurrenceEquals(parseRRule(eventEntity.getRecurrence()), eventResponse.getRecurrence());

    }

    private static Event generateEventEntity() {
        return Event.builder()
                .id(UUID.randomUUID())
                .title("Test title")
                .description("Test description")
                .dateTime(LocalDateTime.now().plusDays(3))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .imageKey("image.webp")
                .organizerId(USER_ID)
                .recurrence("FREQ=MONTHLY;BYMONTHDAY=15,30;UNTIL=20251231T235959Z")
                .build();
    }

    @NotNull
    private static List<EventCategories> generateEventCategories(UUID eventId) {
        return List.of(
                generateEventCategory("Soccer", eventId),
                generateEventCategory("Baseball", eventId),
                generateEventCategory("Hiking", eventId));
    }

    private static EventCategories generateEventCategory(String Soccer, UUID eventId) {
        return EventCategories.builder()
                .id(UUID.randomUUID())
                .activityType(Soccer)
                .eventId(eventId)
                .isNew(true)
                .build();
    }
}