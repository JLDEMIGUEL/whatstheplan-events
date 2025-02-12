package com.whatstheplan.events.services;

import com.whatstheplan.events.exceptions.EventNotFoundException;
import com.whatstheplan.events.exceptions.UploadImageToS3Exception;
import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.CategoryRepository;
import com.whatstheplan.events.repository.EventCategoriesRepository;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final S3Service s3Service;
    private final EventsRepository eventsRepository;
    private final CategoryRepository categoryRepository;
    private final EventCategoriesRepository eventCategoryRepository;

    public Mono<EventResponse> findById(UUID eventId) {
        return eventsRepository.findById(eventId)
                .doOnSuccess(event -> log.info("Found event with id {} and data {}", eventId, event))
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with id: " + eventId)))
                .flatMap(event -> eventCategoryRepository.findAllByEventId(eventId)
                        .doOnError(ex -> {
                            throw new RuntimeException("Unable to retrieve event categories for event id: " + eventId);
                        })
                        .flatMap(eventCategory -> categoryRepository.findById(eventCategory.getCategoryId()))
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
                .doOnNext(imagePath::set)
                .flatMap(request::toNewEntity)
                .doOnSuccess(entity -> log.info("Saving event with data: {}", entity))
                .flatMap(eventsRepository::insert)
                .doOnSuccess(savedEvent -> log.info("Event inserted into repository with ID: {}", savedEvent.getId()))
                .flatMap(savedEvent -> Flux.fromIterable(request.getActivityTypes())
                        .flatMap(activityType ->
                                categoryRepository.findByName(activityType)
                                        .switchIfEmpty(
                                                categoryRepository.save(Category.from(activityType))
                                        )
                        )
                        .collectList()
                        .flatMap(savedCategories ->
                                Flux.fromIterable(savedCategories)
                                        .flatMap(category ->
                                                eventCategoryRepository.save(
                                                        EventCategories.from(savedEvent.getId(), category.getId())
                                                )
                                        )
                                        .collectList()
                                        .thenReturn(savedCategories)
                        )
                        .doOnSuccess(savedCategories -> log.info("Event categories saved successfully: {}", savedCategories))
                        .map(savedCategories ->
                                EventResponse.fromEntity(savedEvent, savedCategories)
                        ))
                .doOnError(ex -> log.error("Error saving event", ex))
                .onErrorResume(ex ->
                        s3Service.deleteFile(imagePath.get())
                                .then(Mono.error(new UploadImageToS3Exception("Error while processing image", ex)))
                );
    }

    public Mono<EventResponse> updateEvent(UUID eventId, EventRequest request, Optional<FilePart> image) {
        return eventsRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with id: " + eventId)))
                .flatMap(event -> image
                        .map(filePart -> updateEventAndImage(event, request, filePart))
                        .orElseGet(() -> updateJustEvent(event, request, event.getImageKey())));
    }

    private Mono<EventResponse> updateJustEvent(Event event, EventRequest request, String imageKey) {
        return request.toUpdateEntity(event.getId(), imageKey)
                .doOnSuccess(entity -> log.info("Updating event with data: {}", entity))
                .flatMap(eventsRepository::update)
                .doOnSuccess(updatedEvent -> log.info("Event updated in repository with ID: {}", updatedEvent.getId()))
                .flatMap(updatedEvent ->
                        eventCategoryRepository.deleteAllByEventId(event.getId())
                                .then(
                                        Flux.fromIterable(request.getActivityTypes())
                                                .flatMap(activityType ->
                                                        categoryRepository.findByName(activityType)
                                                                .switchIfEmpty(
                                                                        categoryRepository.save(Category.from(activityType))
                                                                )
                                                )
                                                .collectList()
                                                .flatMap(categories ->
                                                        Flux.fromIterable(categories)
                                                                .flatMap(category ->
                                                                        eventCategoryRepository.save(
                                                                                EventCategories.from(updatedEvent.getId(), category.getId())
                                                                        )
                                                                )
                                                                .collectList()
                                                                .thenReturn(categories)
                                                )
                                )
                                .map(categories -> EventResponse.fromEntity(updatedEvent, categories))
                )
                .doOnSuccess(savedCategories -> log.info("Event categories updated successfully: {}", savedCategories))
                .doOnError(ex -> log.error("Error updating event", ex));
    }


    private Mono<EventResponse> updateEventAndImage(Event event, EventRequest request, FilePart newImage) {
        AtomicReference<String> newImagePathRef = new AtomicReference<>();
        String oldImagePath = event.getImageKey();

        return s3Service.uploadFile(newImage)
                .doOnNext(newImagePathRef::set)
                .flatMap(newImagePath -> updateJustEvent(event, request, newImagePath))
                .flatMap(response ->
                        s3Service.deleteFile(oldImagePath)
                                .thenReturn(response)
                )
                .onErrorResume(ex ->
                        s3Service.deleteFile(newImagePathRef.get())
                                .then(Mono.error(new UploadImageToS3Exception("Error updating image", ex)))
                );
    }

    public Mono<Void> deleteById(UUID eventId) {
        return eventsRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with id: " + eventId)))
                .flatMap(event ->
                        s3Service.deleteFile(event.getImageKey())
                                .doOnError(error -> log.error("Error deleting image for event {}: {}", eventId, error.getMessage()))
                                .onErrorResume(error -> Mono.empty())
                )
                .then(eventsRepository.deleteById(eventId))
                .then(eventCategoryRepository.deleteAllByEventId(eventId))
                .doOnSuccess(e -> log.info("Successfully deleted event {} and its associated categories", eventId))
                .doOnError(error -> log.error("Error deleting event {}: {}", eventId, error.getMessage(), error));
    }
}
