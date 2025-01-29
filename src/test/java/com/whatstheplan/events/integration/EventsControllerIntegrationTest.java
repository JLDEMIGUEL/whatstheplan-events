package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fortuna.ical4j.model.WeekDay.Day.MO;
import static net.fortuna.ical4j.model.WeekDay.Day.TH;
import static net.fortuna.ical4j.transform.recurrence.Frequency.DAILY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventsControllerIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private S3AsyncClient s3Client;

    private static final ByteArrayResource IMAGE = new ByteArrayResource("fake-image-content".getBytes()) {
        @Override
        public String getFilename() {
            return "event-image.png";
        }
    };

    @Test
    void whenANewEventCreationRequest_thenShouldStoreEventAndCategoriesAndImage() {
        // given
        EventRequest request = EventRequest.builder()
                .title("Test title")
                .description("Test description")
                .dateTime(LocalDateTime.now().plusDays(3))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .recurrence(EventRequest.RecurrenceRequest.builder()
                        .frequency(DAILY.name())
                        .interval(1)
                        .byDays(List.of(MO.name(), TH.name()))
                        .until(LocalDateTime.now().plusYears(1))
                        .build())
                .activityTypes(List.of("Soccer", "Baseball", "Hiking"))
                .build();

        CompletableFuture<PutObjectResponse> futureResponse =
                CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("mock-etag").build());

        given(s3Client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .willReturn(futureResponse);

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
    void whenANewEventCreationRequestWithMissingToken_thenWillReturnUnauthorized() {
        webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus().isUnauthorized();
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
        //assertThat(eventResponse.getRecurrence()).isEqualTo(""); TODO
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(eventResponse.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(eventResponse.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();
        assertThat(eventResponse.getActivityTypes()).containsAll(request.getActivityTypes());

        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(request.getActivityTypes().size());
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
        //assertThat(event.getRecurrence()).isEqualTo(""); TODO
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
}