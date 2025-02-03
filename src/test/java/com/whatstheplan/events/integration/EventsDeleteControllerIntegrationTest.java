package com.whatstheplan.events.integration;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.response.ErrorResponse;
import com.whatstheplan.events.testconfig.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventCategories;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateEventEntity;
import static com.whatstheplan.events.testconfig.utils.S3MockUtils.mockS3DeleteObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventsDeleteControllerIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private S3AsyncClient s3Client;

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenANewEventDeleteRequest_thenShouldDeleteEventFromDatabaseAndImage(
            Event event,
            List<EventCategories> eventCategories) {
        // given
        eventsRepository.insert(event).block();
        eventsCategoriesRepository.saveAll(eventCategories).collectList().block();

        mockS3DeleteObject(s3Client);

        // when - then
        webTestClient
                .mutateWith(JWT)
                .delete()
                .uri("/events/" + event.getId())
                .exchange()
                .expectStatus().isOk();

        Event deletedEvent = eventsRepository.findById(event.getId()).block();
        assertThat(deletedEvent).isNull();

        verify(s3Client, times(1))
                .deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @MethodSource("provideEventEntities")
    void whenANewEventDeleteRequestAndDeleteImageFails_thenShouldDeleteEventFromDatabase(
            Event event,
            List<EventCategories> eventCategories) {
        // given
        eventsRepository.insert(event).block();
        eventsCategoriesRepository.saveAll(eventCategories).collectList().block();

        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Error deleting image")));

        // when - then
        webTestClient
                .mutateWith(JWT)
                .delete()
                .uri("/events/" + event.getId())
                .exchange()
                .expectStatus().isOk();

        Event deletedEvent = eventsRepository.findById(event.getId()).block();
        assertThat(deletedEvent).isNull();

        verify(s3Client, times(1))
                .deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void whenANewEventDeleteRequestWithWrongEventId_thenShouldReturnBadRequest() {
        // given
        UUID wrongEventId = UUID.randomUUID();

        // when - then
        webTestClient
                .mutateWith(JWT)
                .delete()
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
    void whenANewEventDeleteRequestWithMissingRole_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .mutateWith(JWT_NO_ROLE)
                .delete()
                .uri("/events")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenANewEventDeleteRequestWithMissingToken_thenWillReturnUnauthorized() {
        // given - when - then
        webTestClient
                .delete()
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

}