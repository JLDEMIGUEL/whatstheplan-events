package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import com.whatstheplan.events.testconfig.utils.DataMockUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventEntity;
import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCategories;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCreationRequestNotRecurrent;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCreationRequestRecurrent;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateImage;
import static com.whatstheplan.events.testconfig.utils.S3MockUtils.mockS3DeleteObject;
import static com.whatstheplan.events.testconfig.utils.S3MockUtils.mockS3PutObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventsUpdateControllerIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private S3AsyncClient s3Client;

    private static final ByteArrayResource NEW_IMAGE = generateImage("new_fake-image-content".getBytes(), "new_event-image.png");

    @ParameterizedTest
    @MethodSource("provideEventEntitiesAndRequest")
    void whenANewEventUpdateRequestWithNoImageUpdate_thenShouldStoreEventAndCategoriesAndImage(
            Event event,
            List<Category> categories,
            EventRequest request) {
        // given
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .put()
                .uri("/events/" + event.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    assertEventResponse(request, event.getImageKey(), response.getResponseBody().get(0));

                    List<Event> events = eventsRepository.findAll().collectList().block();
                    List<EventCategories> eventCategories = eventCategoriesRepository.findAll().collectList().block();
                    List<Category> categoryEntities = categoryRepository.findAllById(
                                    eventCategories.stream().map(EventCategories::getCategoryId).toList())
                            .collectList().block();
                    assertEventEntity(request, event.getImageKey(), events.get(0), categoryEntities);

                    verify(s3Client, times(0))
                            .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                    verify(s3Client, times(0))
                            .deleteObject(any(DeleteObjectRequest.class));
                });
    }

    @ParameterizedTest
    @MethodSource("provideEventEntitiesAndRequest")
    void whenANewEventUpdateRequestWithImageUpdate_thenShouldUploadImageAndStoreEventAndCategoriesAndImage(
            Event event,
            List<Category> categories,
            EventRequest request) {
        // given
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();
        mockS3PutObject(s3Client);
        mockS3DeleteObject(s3Client);

        // when - then
        webTestClient
                .mutateWith(JWT)
                .put()
                .uri("/events/" + event.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", NEW_IMAGE))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    assertEventResponse(request, NEW_IMAGE.getFilename(), response.getResponseBody().get(0));

                    List<Event> events = eventsRepository.findAll().collectList().block();
                    List<EventCategories> eventCategories = eventCategoriesRepository.findAll().collectList().block();
                    List<Category> categoryEntities = categoryRepository.findAllById(
                                    eventCategories.stream().map(EventCategories::getCategoryId).toList())
                            .collectList().block();
                    assertEventEntity(request, NEW_IMAGE.getFilename(), events.get(0), categoryEntities);

                    verify(s3Client, times(1))
                            .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                    verify(s3Client, times(1))
                            .deleteObject(any(DeleteObjectRequest.class));
                });
    }

    @Test
    void whenANewEventUpdateRequestFailsToSaveInDatabase_thenWillDeleteImageAndReturnBadRequest() {
        // given
        Event event = generateEventEntity();
        List<Category> categories = generateEventCategories(event.getId());
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();


        EventRequest request = generateEventCreationRequestRecurrent();

        mockS3PutObject(s3Client);
        mockS3DeleteObject(s3Client);

        when(eventsRepository.update(any(Event.class)))
                .thenThrow(new DataAccessResourceFailureException("Error saving entity in database"));

        // when - then
        webTestClient
                .mutateWith(JWT)
                .put()
                .uri("/events/" + event.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", NEW_IMAGE))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBodyList(ErrorResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    ErrorResponse errorResponse = response.getResponseBody().get(0);
                    assertThat(errorResponse.getReason()).isEqualTo("Error while processing image");

                    verify(s3Client, times(1))
                            .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                    verify(s3Client, times(1))
                            .deleteObject(any(DeleteObjectRequest.class));

                    Event savedEvent = eventsRepository.findById(event.getId()).block();
                    List<EventCategories> eventCategories = eventCategoriesRepository.findAll().collectList().block();
                    List<Category> savedCategories = categoryRepository.findAllById(
                                    eventCategories.stream().map(EventCategories::getCategoryId).toList())
                            .collectList().block();
                    assertEventEntity(event, savedEvent, categories, savedCategories);
                });
    }

    @ParameterizedTest
    @MethodSource("invalidRequestProvider")
    void whenANewEventUpdateRequestWithBadParameters_thenWillReturnBadRequest(
            EventRequest request,
            ByteArrayResource content,
            List<String> expectedErrorMessages
    ) {
        // given
        Event event = generateEventEntity();
        List<Category> categories = generateEventCategories(event.getId());
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .put()
                .uri("/events/" + event.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", content)).exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(response -> {
                    ErrorResponse responseBody = response.getResponseBody();
                    assertThat(responseBody.getReason()).contains(expectedErrorMessages);
                });
    }

    @Test
    void whenANewEventUpdateRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given
        Event event = generateEventEntity();
        List<Category> categories = generateEventCategories(event.getId());
        eventsRepository.insert(event).block();
        categoryRepository.saveAll(categories).collectList().block();
        eventCategoriesRepository.saveAll(
                        categories.stream().map(c -> EventCategories.from(event.getId(), c.getId())).toList())
                .collectList().block();

        EventRequest request = DataMockUtils.generateEventCreationRequestRecurrent();

        // when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .put()
                .uri("/events/" + event.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", NEW_IMAGE))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventUpdateRequestWithMissingToken_thenWillReturnUnauthorized() {
        webTestClient
                .get()
                .uri("/events/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }


    private static Stream<Arguments> provideEventEntitiesAndRequest() {
        Event event = generateEventEntity();
        List<Category> categories = generateEventCategories(event.getId());
        EventRequest recurrentRequest = generateEventCreationRequestRecurrent();
        EventRequest nonRecurrentRequest = generateEventCreationRequestNotRecurrent();
        return Stream.of(
                Arguments.of(event, categories, recurrentRequest),
                Arguments.of(event, List.of(), recurrentRequest),
                Arguments.of(event, categories, nonRecurrentRequest),
                Arguments.of(event, List.of(), nonRecurrentRequest)
        );
    }

    private static Stream<Arguments> invalidRequestProvider() {
        EventRequest validRequest = EventRequest.builder()
                .title("Valid Title")
                .description("Valid Description")
                .dateTime(LocalDateTime.now().plusDays(2))
                .duration(Duration.ofHours(2))
                .location("Valid Location")
                .capacity(10)
                .recurrence(null)
                .build();

        ByteArrayResource validImage = generateImage(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "valid.png");

        return Stream.of(
                // --- Title is blank ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .title("")
                                .build(),
                        validImage,
                        List.of("Title is required.")
                ),
                // --- Description is blank ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .description("")
                                .build(),
                        validImage,
                        List.of("Description is required.")
                ),
                // --- DateTime is null ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .dateTime(null)
                                .build(),
                        validImage,
                        List.of("Date and time must be specified.")
                ),
                // --- DateTime is in the past ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .dateTime(LocalDateTime.now().minusDays(1))
                                .build(),
                        validImage,
                        List.of("Event date must be in the future.")
                ),
                // --- Duration is null ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .duration(null)
                                .build(),
                        validImage,
                        List.of("Duration is required.")
                ),
                // --- Location is blank ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .location("")
                                .build(),
                        validImage,
                        List.of("Location is required.")
                ),
                // --- Capacity is < 1 ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .capacity(0)
                                .build(),
                        validImage,
                        List.of("Capacity must be at least 1.")
                ),
                // --- Invalid recurrence frequency ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .recurrence(Recurrence.builder()
                                        .frequency("HOURLY") // invalid => violates pattern
                                        .interval(1)
                                        .byDays(List.of("MO"))
                                        .build())
                                .build(),
                        validImage,
                        List.of("Invalid recurrence frequency.")
                ),
                // --- Recurrence interval <= 0 ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .recurrence(Recurrence.builder()
                                        .frequency("DAILY")
                                        .interval(0)
                                        .byDays(List.of("MO"))
                                        .build())
                                .build(),
                        validImage,
                        List.of("Interval must be a positive number.")
                ),
                // --- Recurrence byDays is empty ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .recurrence(Recurrence.builder()
                                        .frequency("DAILY")
                                        .interval(1)
                                        .byDays(List.of()) // violates @Size(min=1)
                                        .build())
                                .build(),
                        validImage,
                        List.of("At least one day must be specified.")
                ),
                // --- Recurrence until + count both set ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .recurrence(Recurrence.builder()
                                        .frequency("DAILY")
                                        .interval(1)
                                        .byDays(List.of("MO"))
                                        .until(LocalDateTime.now().plusDays(10))
                                        .count(5)
                                        .build())
                                .build(),
                        validImage,
                        List.of("Only one of 'until' or 'count' can be specified.")
                ),
                // --- Invalid image extension (GIF) ---
                arguments(
                        validRequest,
                        generateImage(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "invalid.gif"),
                        List.of("Invalid image format. Allowed: PNG, JPG, JPEG.")
                ),
                // --- Image too large (6MB) ---
                arguments(
                        validRequest,
                        generateImage(new byte[6 * 1024 * 1024], "large-image.png"),
                        List.of("Image size exceeds 5MB.")
                ),
                // --- Multiple validation errors ---
                arguments(
                        DataMockUtils.generateEventCreationRequestRecurrent().toBuilder()
                                .title("")                          // Title is blank
                                .description("")                    // Description is blank
                                .dateTime(null)                     // DateTime is null
                                .capacity(0)                        // Capacity is less than 1
                                .recurrence(Recurrence.builder()
                                        .frequency("INVALID")            // Invalid recurrence frequency
                                        .interval(0)                     // Recurrence interval <= 0
                                        .byDays(Collections.emptyList()) // Recurrence byDays is empty
                                        .until(LocalDateTime.now())       // Both 'until' and 'count' set
                                        .count(5)
                                        .build())
                                .build(),
                        validImage,
                        List.of(
                                "Title is required.",
                                "Description is required.",
                                "Date and time must be specified.",
                                "Capacity must be at least 1.",
                                "Invalid recurrence frequency.",
                                "Interval must be a positive number.",
                                "At least one day must be specified.",
                                "Only one of 'until' or 'count' can be specified."
                        )
                )
        );
    }

}