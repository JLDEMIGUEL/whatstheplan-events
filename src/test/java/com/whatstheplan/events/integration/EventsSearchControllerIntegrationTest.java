package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventCategoriesRepository;
import com.whatstheplan.events.repository.EventsRepository;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import com.whatstheplan.events.testconfig.utils.DataMockUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.whatstheplan.events.testconfig.utils.DataMockUtils.TODAY;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

class EventsSearchControllerIntegrationTest extends BaseIntegrationTest {


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private EventCategoriesRepository eventCategoriesRepository;

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void searchWithFilters_ParameterizedTest(List<EventData> eventDataList,
                                             MultiValueMap<String, String> filterParams,
                                             int expectedSize,
                                             Consumer<List<EventResponse>> assertions) {
        eventDataList.forEach(eventData -> {
            Event event = eventData.event();
            eventsRepository.insert(event).block();

            List<Category> categories = eventData.categories().stream()
                    .map(DataMockUtils::generateEventCategory)
                    .toList();

            categories.forEach(category ->
                    categoryRepository.findByName(category.getName())
                            .switchIfEmpty(categoryRepository.save(category))
                            .doOnSuccess(c -> category.setId(c.getId()))
                            .block());

            List<EventCategories> eventCategoriesList = categories.stream()
                    .map(category -> EventCategories.from(event.getId(), category.getId()))
                    .toList();

            eventCategoriesRepository.saveAll(eventCategoriesList).collectList().block();
        });

        webTestClient.mutateWith(JWT)
                .get()
                .uri(uriBuilder -> uriBuilder.path("/events/search")
                        .queryParams(filterParams)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .value(responses -> {
                    assertThat(responses).hasSize(expectedSize);
                    assertions.accept(responses);
                });
    }

    @Test
    void whenANewEventRetrievalRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .get()
                .uri("/events/search")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventRetrievalRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .get()
                .uri("/events/search")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of(
                        List.of(
                                new EventData(
                                        createEvent(e -> e.location("Paris")),
                                        List.of("Soccer")
                                ),
                                new EventData(
                                        createEvent(e -> e.location("London")),
                                        List.of("Hiking")
                                )
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("location", "Paris");
                        }},
                        1,
                        (Consumer<List<EventResponse>>) (List<EventResponse> responses) -> {
                            assertThat(responses.get(0).getLocation()).isEqualTo("Paris");
                            assertThat(responses.get(0).getActivityTypes()).contains("Soccer");
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> {
                                }), List.of("Soccer")),
                                new EventData(createEvent(e -> {
                                }), List.of("Hiking")),
                                new EventData(createEvent(e -> {
                                }), List.of("Soccer", "Hiking")),
                                new EventData(createEvent(e -> {
                                }), List.of("Baseball"))
                        ),
                        new LinkedMultiValueMap<>() {{
                            addAll("activityTypes", List.of("Soccer", "Hiking"));
                        }},
                        3,
                        (Consumer<List<EventResponse>>) (List<EventResponse> responses) -> {
                            List<String> activityTypes = responses.stream()
                                    .flatMap(r -> r.getActivityTypes().stream())
                                    .distinct()
                                    .toList();
                            assertThat(activityTypes).containsExactlyInAnyOrder("Soccer", "Hiking");
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.capacity(5)), List.of()),
                                new EventData(createEvent(e -> e.capacity(15)), List.of()),
                                new EventData(createEvent(e -> e.capacity(25)), List.of())
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("capacityMin", "10");
                            add("capacityMax", "20");
                        }},
                        1,
                        (Consumer<List<EventResponse>>) (List<EventResponse> responses) -> {
                            assertThat(responses.get(0).getCapacity()).isEqualTo(15);
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(
                                        createEvent(e -> e.dateTime(TODAY.plusDays(1))), List.of()
                                ),
                                new EventData(
                                        createEvent(e -> e.dateTime(TODAY.plusDays(4))), List.of()
                                )
                        ),
                        new LinkedMultiValueMap<String, String>() {{
                            add("dateTimeFrom", TODAY.format(ISO_DATE_TIME));
                            add("dateTimeTo", TODAY.plusDays(2).format(ISO_DATE_TIME));
                        }},
                        1,
                        (Consumer<List<EventResponse>>) (List<EventResponse> responses) -> {
                            LocalDateTime now = TODAY;
                            assertThat(responses.get(0).getDateTime())
                                    .isBetween(now, now.plusDays(2));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.duration(Duration.ofMinutes(30))), List.of()),
                                new EventData(createEvent(e -> e.duration(Duration.ofHours(1))), List.of()),
                                new EventData(createEvent(e -> e.duration(Duration.ofHours(2))), List.of()),
                                new EventData(createEvent(e -> e.duration(Duration.ofHours(3))), List.of())
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("durationFrom", "PT1H");
                            add("durationTo", "PT2H");
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .extracting(EventResponse::getDuration)
                                    .containsExactlyInAnyOrder(Duration.ofHours(1), Duration.ofHours(2));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.location("Paris")), List.of("Soccer")),
                                new EventData(createEvent(e -> e.location("Paris")), List.of("Hiking")),
                                new EventData(createEvent(e -> e.location("London")), List.of("Soccer"))
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("location", "Paris");
                            add("activityTypes", "Soccer");
                        }},
                        1,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses.get(0).getLocation()).isEqualTo("Paris");
                            assertThat(responses.get(0).getActivityTypes()).contains("Soccer");
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.dateTime(TODAY)), List.of()),
                                new EventData(createEvent(e -> e.dateTime(TODAY.plusMonths(2))), List.of())
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("dateTimeFrom", TODAY.format(ISO_DATE_TIME));
                            add("dateTimeTo", TODAY.plusMonths(2).format(ISO_DATE_TIME));
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .extracting(EventResponse::getDateTime)
                                    .containsExactlyInAnyOrder(TODAY, TODAY.plusMonths(2));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.capacity(10)), List.of()),
                                new EventData(createEvent(e -> e.capacity(15)), List.of())
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("capacityMin", "10");
                            add("capacityMax", "10");
                        }},
                        1,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses.get(0).getCapacity()).isEqualTo(10);
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.dateTime(TODAY.plusDays(1))), List.of()),
                                new EventData(createEvent(e -> e.dateTime(TODAY.plusMonths(3))), List.of())
                        ),
                        new LinkedMultiValueMap<>(),
                        1,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses.get(0).getDateTime())
                                    .isBefore(TODAY.plusMonths(2));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> {
                                    e.location("Berlin")
                                            .duration(Duration.ofHours(2))
                                            .capacity(15)
                                            .dateTime(TODAY.plusDays(5));
                                }), List.of("Swimming")),
                                new EventData(createEvent(e -> {
                                    e.location("Berlin")
                                            .duration(Duration.ofHours(1))
                                            .capacity(20)
                                            .dateTime(TODAY.plusDays(3));
                                }), List.of("Swimming", "Yoga"))
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("location", "Berlin");
                            add("durationFrom", "PT1H");
                            add("durationTo", "PT2H");
                            add("capacityMin", "15");
                            add("capacityMax", "20");
                            add("dateTimeFrom", TODAY.plusDays(1).format(ISO_DATE_TIME));
                            add("dateTimeTo", TODAY.plusDays(10).format(ISO_DATE_TIME));
                            add("activityTypes", "Swimming");
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .allMatch(r -> r.getLocation().equals("Berlin") &&
                                            r.getCapacity() >= 15 &&
                                            r.getCapacity() <= 20 &&
                                            r.getActivityTypes().contains("Swimming"));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> {
                                }), List.of("Soccer")),
                                new EventData(createEvent(e -> {
                                }), List.of("Hiking")),
                                new EventData(createEvent(e -> {
                                }), List.of("Soccer", "Hiking")),
                                new EventData(createEvent(e -> {
                                }), List.of("Baseball"))
                        ),
                        new LinkedMultiValueMap<>() {{
                            addAll("activityTypes", List.of("Soccer", "Hiking"));
                        }},
                        3,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses.stream()
                                    .flatMap(r -> r.getActivityTypes().stream())
                                    .distinct())
                                    .contains("Soccer", "Hiking");
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e ->
                                        e.dateTime(TODAY.plusMonths(3))), List.of())
                        ),
                        new LinkedMultiValueMap<>(),
                        0,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses).isEmpty();
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> e.duration(Duration.ofHours(1))), List.of()),
                                new EventData(createEvent(e -> e.duration(Duration.ofHours(2))), List.of())
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("durationFrom", "PT1H");
                            add("durationTo", "PT2H");
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .extracting(EventResponse::getDuration)
                                    .containsExactlyInAnyOrder(Duration.ofHours(1), Duration.ofHours(2));
                        }
                ),
                Arguments.of(
                        List.of(
                                new EventData(createEvent(e -> {
                                }), List.of("Soccer")),
                                new EventData(createEvent(e -> {
                                }), List.of("Hiking"))
                        ),
                        new LinkedMultiValueMap<>() {{
                            add("activityTypes", "");
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .extracting(EventResponse::getActivityTypes)
                                    .containsExactlyInAnyOrder(List.of("Soccer"), List.of("Hiking"));
                        }
                )
        );
    }

    private static Event createEvent(Consumer<Event.EventBuilder> customizer) {
        Event.EventBuilder builder = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .description("Test Description")
                .dateTime(TODAY.plusDays(1))
                .duration(Duration.ofHours(1))
                .location("Default Location")
                .capacity(10)
                .imageKey("image.webp")
                .organizerId(USER_ID)
                .recurrence(null)
                .isNew(true);
        customizer.accept(builder);
        return builder.build();
    }

    record EventData(Event event, List<String> categories) {
    }
}