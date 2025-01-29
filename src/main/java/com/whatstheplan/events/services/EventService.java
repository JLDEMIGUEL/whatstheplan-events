package com.whatstheplan.events.services;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final S3Service s3Service;
    private final EventsRepository eventsRepository;
    private final EventsCategoriesRepository eventsCategoriesRepository;

    public Mono<EventResponse> saveEvent(EventRequest request, FilePart image) {
        return s3Service.uploadFile(image)
                .flatMap(request::toNewEntity)
                .doOnSuccess(entity -> log.info("Saving event with data: {}", entity))
                .flatMap(eventsRepository::insert)
                .doOnSuccess(savedEvent -> log.info("Event inserted into repository with ID: {}", savedEvent.getId()))
                .doOnError(error -> log.error("Failed to insert Event into repository", error))
                .flatMap(savedEvent -> eventsCategoriesRepository.saveAll(
                                EventCategories.from(request.getActivityTypes(), savedEvent.getId()))
                        .doOnSubscribe(sub -> log.info("Saving event categories for event ID: {}", savedEvent.getId()))
                        .collectList()
                        .doOnSuccess(savedCategories -> log.info("Event categories saved successfully: {}", savedCategories))
                        .doOnError(error -> log.error("Failed to save event categories", error))
                        .map(savedCategories -> EventResponse.fromEntity(savedEvent, savedCategories))
                );
    }
}
