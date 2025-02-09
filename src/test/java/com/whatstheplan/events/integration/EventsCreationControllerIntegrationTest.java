package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
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
import java.util.stream.Stream;

import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventEntity;
import static com.whatstheplan.events.testconfig.utils.AssertionUtils.assertEventResponse;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCreationRequestNotRecurrent;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCreationRequestRecurrent;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateImage;
import static com.whatstheplan.events.testconfig.utils.S3MockUtils.mockS3DeleteObject;
import static com.whatstheplan.events.testconfig.utils.S3MockUtils.mockS3PutObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventsCreationControllerIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private S3AsyncClient s3Client;

    private static final ByteArrayResource IMAGE = generateImage("fake-image-content".getBytes(), "event-image.png");

    @ParameterizedTest
    @MethodSource("provideEventRequests")
    void whenANewEventCreationRequest_thenShouldStoreEventAndCategoriesAndImage(EventRequest request) {
        // given
        mockS3PutObject(s3Client);

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", IMAGE))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(EventResponse.class)
                .hasSize(1)
                .consumeWith(response -> {
                    assertEventResponse(request, IMAGE.getFilename(), response.getResponseBody().get(0));

                    List<Event> events = eventsRepository.findAll().collectList().block();
                    List<EventCategories> eventCategories = eventCategoriesRepository.findAll().collectList().block();
                    List<Category> categories = categoryRepository.findAllById(
                                    eventCategories.stream().map(EventCategories::getCategoryId).toList())
                            .collectList().block();
                    assertEventEntity(request, IMAGE.getFilename(), events.get(0), categories);

                    verify(s3Client, times(1))
                            .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                });
    }

    @Test
    void whenANewEventCreationRequestFailsToSaveInDatabase_thenWillDeleteImageAndReturnBadRequest() {
        // given
        EventRequest request = generateEventCreationRequestRecurrent();

        mockS3PutObject(s3Client);
        mockS3DeleteObject(s3Client);

        when(eventsRepository.insert(any(Event.class)))
                .thenThrow(new DataAccessResourceFailureException("Error saving entity in database"));

        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", IMAGE))
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
                });
    }

    @ParameterizedTest
    @MethodSource("invalidRequestProvider")
    void whenANewEventCreationRequestWithBadParameters_thenWillReturnBadRequest(
            EventRequest request,
            ByteArrayResource content,
            List<String> expectedErrorMessages
    ) {
        // when - then
        webTestClient
                .mutateWith(JWT)
                .post()
                .uri("/events")
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
    void whenANewEventCreationRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given
        EventRequest request = generateEventCreationRequestRecurrent();

        // when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .post()
                .uri("/events")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters
                        .fromMultipartData("event", request)
                        .with("image", IMAGE))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventCreationRequestWithMissingToken_thenWillReturnUnauthorized() {
        webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    public static Stream<Arguments> provideEventRequests() {
        return Stream.of(
                Arguments.of(generateEventCreationRequestNotRecurrent()),
                Arguments.of(generateEventCreationRequestRecurrent())
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
                        generateEventCreationRequestRecurrent().toBuilder()
                                .title("")
                                .build(),
                        validImage,
                        List.of("Title is required.")
                ),
                // --- Description is blank ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .description("")
                                .build(),
                        validImage,
                        List.of("Description is required.")
                ),
                // --- DateTime is null ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .dateTime(null)
                                .build(),
                        validImage,
                        List.of("Date and time must be specified.")
                ),
                // --- DateTime is in the past ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .dateTime(LocalDateTime.now().minusDays(1))
                                .build(),
                        validImage,
                        List.of("Event date must be in the future.")
                ),
                // --- Duration is null ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .duration(null)
                                .build(),
                        validImage,
                        List.of("Duration is required.")
                ),
                // --- Location is blank ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .location("")
                                .build(),
                        validImage,
                        List.of("Location is required.")
                ),
                // --- Capacity is < 1 ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
                                .capacity(0)
                                .build(),
                        validImage,
                        List.of("Capacity must be at least 1.")
                ),
                // --- Invalid recurrence frequency ---
                arguments(
                        generateEventCreationRequestRecurrent().toBuilder()
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
                        generateEventCreationRequestRecurrent().toBuilder()
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
                        generateEventCreationRequestRecurrent().toBuilder()
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
                        generateEventCreationRequestRecurrent().toBuilder()
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
                        generateEventCreationRequestRecurrent().toBuilder()
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