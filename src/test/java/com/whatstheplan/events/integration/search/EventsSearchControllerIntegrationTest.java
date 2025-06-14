package com.whatstheplan.events.integration.search;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
                .expectBody(new ParameterizedTypeReference<RestPage<EventResponse>>() {
                })
                .value(page -> {
                    assertThat(page.getPageable().getPageNumber()).isZero();
                    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(expectedSize);

                    List<EventResponse> responses = page.getContent();
                    assertThat(responses).hasSize(expectedSize);
                    assertThat(responses)
                            .extracting(EventResponse::getDateTime)
                            .isSorted();
                    assertions.accept(responses);
                });
    }

    @ParameterizedTest
    @MethodSource("providePaginationTestCases")
    void searchWithPagination_ParameterizedTest(int page, int size, int expectedContentSize, long totalElements) {
        List<Event> eventDataList = generateTestEvents(15);
        eventDataList.forEach(event -> eventsRepository.insert(event).block());

        webTestClient.mutateWith(JWT)
                .get()
                .uri(uriBuilder -> uriBuilder.path("/events/search")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<RestPage<EventResponse>>() {
                })
                .value(pageResponse -> {
                    assertThat(pageResponse.getNumber()).isEqualTo(page);
                    assertThat(pageResponse.getSize()).isEqualTo(size);
                    assertThat(pageResponse.getTotalElements()).isEqualTo(totalElements);
                    assertThat(pageResponse.getContent()).hasSize(expectedContentSize);

                    assertThat(pageResponse.getContent())
                            .extracting(EventResponse::getDateTime)
                            .isSorted();
                });
    }

    private static Stream<Arguments> providePaginationTestCases() {
        return Stream.of(
                // page | size | expectedContentSize | totalElements
                Arguments.of(0, 5, 5, 15),    // First page
                Arguments.of(1, 5, 5, 15),    // Second page
                Arguments.of(2, 5, 5, 15),    // Third page
                Arguments.of(3, 5, 0, 15),    // Page beyond data range
                Arguments.of(0, 20, 15, 15)  // Page size larger than dataset
        );
    }

    private List<Event> generateTestEvents(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEvent(e -> e.dateTime(TODAY.plusDays(1).plusMinutes(i))))
                .collect(Collectors.toList());
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
                                new EventData(createEvent(e -> e.dateTime(TODAY.plusDays(1))), List.of()),
                                new EventData(createEvent(e -> e.dateTime(TODAY.plusMonths(13))), List.of())
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
                            add("dateTimeFrom", TODAY.plusDays(1).format(ISO_DATE_TIME));
                            add("dateTimeTo", TODAY.plusDays(10).format(ISO_DATE_TIME));
                            add("activityTypes", "Swimming");
                        }},
                        2,
                        (Consumer<List<EventResponse>>) responses -> {
                            assertThat(responses)
                                    .allMatch(r -> r.getLocation().equals("Berlin") &&
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
                                        e.dateTime(TODAY.plusMonths(13))), List.of())
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
                .isNew(true);
        customizer.accept(builder);
        return builder.build();
    }

    record EventData(Event event, List<String> categories) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
    public static class RestPage<T> extends PageImpl<T> {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RestPage(@JsonProperty("content") List<T> content,
                        @JsonProperty("number") int page,
                        @JsonProperty("size") int size,
                        @JsonProperty("totalElements") long total) {
            super(content, PageRequest.of(page, size), total);
        }

        public RestPage(Page<T> page) {
            super(page.getContent(), page.getPageable(), page.getTotalElements());
        }
    }
}