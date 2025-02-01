package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.jetbrains.annotations.NotNull;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.whatstheplan.events.utils.RecurrenceUtilsTest.assertRecurrenceEquals;
import static net.fortuna.ical4j.model.WeekDay.Day.MO;
import static net.fortuna.ical4j.model.WeekDay.Day.TH;
import static net.fortuna.ical4j.transform.recurrence.Frequency.DAILY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventsCreationControllerIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private S3AsyncClient s3Client;

    private static final ByteArrayResource IMAGE = generateImage("fake-image-content".getBytes(), "event-image.png");

    @Test
    void whenANewEventCreationRequest_thenShouldStoreEventAndCategoriesAndImage() {
        // given
        EventRequest request = generateEventCreationRequest();

        mockS3PutObject();

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
                    assertEventResponse(request, response.getResponseBody());

                    List<Event> events = eventsRepository.findAll().collectList().block();
                    List<EventCategories> eventCategories = eventsCategoriesRepository.findAll().collectList().block();
                    assertEventEntity(request, events, eventCategories);

                    verify(s3Client, times(1))
                            .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                });
    }

    @Test
    void whenANewEventCreationRequestFailsToSaveInDatabase_thenWillDeleteImageAndReturnBadRequest() {
        // given
        EventRequest request = generateEventCreationRequest();

        mockS3PutObject();
        mockS3DeleteObject();

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
        EventRequest request = generateEventCreationRequest();

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


    private static EventRequest generateEventCreationRequest() {
        EventRequest request = EventRequest.builder()
                .title("Test title")
                .description("Test description")
                .dateTime(LocalDateTime.now().plusDays(3))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .recurrence(Recurrence.builder()
                        .frequency(DAILY.name())
                        .interval(1)
                        .byDays(List.of(MO.name(), TH.name()))
                        .until(LocalDateTime.now().plusYears(1))
                        .build())
                .activityTypes(List.of("Soccer", "Baseball", "Hiking"))
                .build();
        return request;
    }

    private void mockS3PutObject() {
        CompletableFuture<PutObjectResponse> futureResponse =
                CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("mock-etag").build());

        given(s3Client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .willReturn(futureResponse);
    }


    private void mockS3DeleteObject() {
        CompletableFuture<DeleteObjectResponse> futureResponse =
                CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());

        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(futureResponse);
    }

    private static void assertEventResponse(EventRequest request, List<EventResponse> events) {
        assertThat(events).isNotNull();
        assertThat(events).hasSize(1);

        EventResponse eventResponse = events.get(0);
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(request.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(request.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(request.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(request.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(IMAGE.getFilename());
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(eventResponse.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(eventResponse.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();

        assertThat(eventResponse.getActivityTypes()).containsAll(request.getActivityTypes());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(request.getActivityTypes().size());

        assertRecurrenceEquals(request.getRecurrence(), eventResponse.getRecurrence());
    }

    private static void assertEventEntity(EventRequest request, List<Event> events, List<EventCategories> eventCategories) {
        assertThat(events).isNotNull();
        assertThat(events).hasSize(1);

        Event event = events.get(0);
        assertThat(event.getId()).isNotNull();
        assertThat(event.getTitle()).isEqualTo(request.getTitle());
        assertThat(event.getDescription()).isEqualTo(request.getDescription());
        assertThat(event.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(event.getDuration()).isEqualTo(request.getDuration());
        assertThat(event.getLocation()).isEqualTo(request.getLocation());
        assertThat(event.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(event.getImageKey()).contains(IMAGE.getFilename());
        assertThat(event.getRecurrence()).contains("FREQ=DAILY",
                "UNTIL=" + LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                "BYDAY=MO,TH");
        assertThat(event.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(event.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(event.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(event.getCreatedDate()).isNotNull();
        assertThat(event.getLastModifiedDate()).isNotNull();

        assertThat(eventCategories.stream().map(EventCategories::getActivityType).toList())
                .containsAll(request.getActivityTypes());
        assertThat(eventCategories.size()).isEqualTo(request.getActivityTypes().size());

        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getEventId()).isEqualTo(event.getId()));
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getId()).isNotNull());
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getCreatedDate()).isNotNull());
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getLastModifiedDate()).isNotNull());
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
                        generateEventCreationRequest().toBuilder()
                                .title("")
                                .build(),
                        validImage,
                        List.of("Title is required.")
                ),
                // --- Description is blank ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .description("")
                                .build(),
                        validImage,
                        List.of("Description is required.")
                ),
                // --- DateTime is null ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .dateTime(null)
                                .build(),
                        validImage,
                        List.of("Date and time must be specified.")
                ),
                // --- DateTime is in the past ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .dateTime(LocalDateTime.now().minusDays(1))
                                .build(),
                        validImage,
                        List.of("Event date must be in the future.")
                ),
                // --- Duration is null ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .duration(null)
                                .build(),
                        validImage,
                        List.of("Duration is required.")
                ),
                // --- Location is blank ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .location("")
                                .build(),
                        validImage,
                        List.of("Location is required.")
                ),
                // --- Capacity is < 1 ---
                arguments(
                        generateEventCreationRequest().toBuilder()
                                .capacity(0)
                                .build(),
                        validImage,
                        List.of("Capacity must be at least 1.")
                ),
                // --- Invalid recurrence frequency ---
                arguments(
                        generateEventCreationRequest().toBuilder()
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
                        generateEventCreationRequest().toBuilder()
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
                        generateEventCreationRequest().toBuilder()
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
                        generateEventCreationRequest().toBuilder()
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
                        generateEventCreationRequest().toBuilder()
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

    @NotNull
    private static ByteArrayResource generateImage(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

}