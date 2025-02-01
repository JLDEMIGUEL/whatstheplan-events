package com.whatstheplan.events.services;

import com.whatstheplan.events.exceptions.EventNotFoundException;
import com.whatstheplan.events.exceptions.UploadImageToS3Exception;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventsCategoriesRepository;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final S3Service s3Service;
    private final EventsRepository eventsRepository;
    private final EventsCategoriesRepository eventsCategoriesRepository;

    public Mono<EventResponse> findById(UUID eventId) {
        return eventsRepository.findById(eventId)
                .doOnSuccess(event -> log.info("Found event with id {} and data {}", eventId, event))
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with id: " + eventId)))
                .flatMap(event -> eventsCategoriesRepository.findAllByEventId(eventId)
                        .doOnError(ex -> {
                            throw new RuntimeException("Unable to retrieve event categories for event id: " + eventId);
                        })
                        .collectList()
                        .map(categories -> EventResponse.fromEntity(event, categories)))
                .doOnSuccess(response -> log.info("Returning event response: {}", response));
    }

    public Mono<EventResponse> saveEvent(EventRequest request, FilePart image) {
        AtomicReference<String> imagePath = new AtomicReference<>();
        return s3Service.uploadFile(image)
                .doOnError(ex -> {
                    throw new UploadImageToS3Exception("Error uploading image to s3", ex);
                })
                .flatMap(uploadedPath -> {
                    imagePath.set(uploadedPath);
                    return request.toNewEntity(uploadedPath);
                })
                .doOnSuccess(entity -> log.info("Saving event with data: {}", entity))
                .flatMap(eventsRepository::insert)
                .doOnSuccess(savedEvent -> log.info("Event inserted into repository with ID: {}", savedEvent.getId()))
                .flatMap(savedEvent -> eventsCategoriesRepository.saveAll(
                                EventCategories.from(request.getActivityTypes(), savedEvent.getId()))
                        .doOnSubscribe(sub -> log.info("Saving event categories for event ID: {}", savedEvent.getId()))
                        .collectList()
                        .doOnSuccess(savedCategories -> log.info("Event categories saved successfully: {}", savedCategories))
                        .doOnError(error -> log.error("Failed to save event categories", error))
                        .map(savedCategories -> EventResponse.fromEntity(savedEvent, savedCategories))
                ).onErrorResume(ex -> s3Service.deleteFile(imagePath.get())
                        .then(Mono.error(new UploadImageToS3Exception("Error while processing image", ex))));
    }
}
