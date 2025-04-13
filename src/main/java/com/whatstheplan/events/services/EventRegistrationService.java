package com.whatstheplan.events.services;

import com.whatstheplan.events.exceptions.DuplicateRegistrationException;
import com.whatstheplan.events.exceptions.EventFullException;
import com.whatstheplan.events.exceptions.EventNotFoundException;
import com.whatstheplan.events.model.entities.Registration;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.CategoryRepository;
import com.whatstheplan.events.repository.EventCategoriesRepository;
import com.whatstheplan.events.repository.EventsRepository;
import com.whatstheplan.events.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.whatstheplan.events.config.RedisConfig.CACHE_TTL;
import static com.whatstheplan.events.utils.Utils.getUserId;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    public static final String IS_REGISTERED_KEY = "registration:exists:";

    private final EventsRepository eventsRepository;
    private final CategoryRepository categoryRepository;
    private final EventCategoriesRepository eventCategoryRepository;
    private final RegistrationRepository registrationRepository;
    private final ReactiveRedisTemplate<String, Boolean> booleanReactiveRedisTemplate;

    public Mono<Boolean> isRegistered(UUID user, UUID eventId) {
        log.info("Checking if user {} is registered to event {}", user, eventId);
        String cacheKey = IS_REGISTERED_KEY + user + ":" + eventId;
        return booleanReactiveRedisTemplate.opsForValue()
                .get(cacheKey)
                .map(Boolean::valueOf)
                .doOnNext(val -> log.info("isRegistered cache hit for key {}", cacheKey))
                .switchIfEmpty(
                        registrationRepository.findByUserIdAndEventId(user, eventId)
                                .map(reg -> true).defaultIfEmpty(false)
                                .flatMap(result ->
                                        booleanReactiveRedisTemplate.opsForValue()
                                                .set(cacheKey, result, CACHE_TTL)
                                                .thenReturn(result))
                );
    }

    public Mono<Void> register(UUID user, UUID eventId) {
        return eventsRepository.findById(eventId)
                .doOnSuccess(event -> log.info("Found event with id {} and data {}", eventId, event))
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with id: " + eventId)))
                .flatMap(event -> Objects.equals(event.getRegistrations(), event.getCapacity()) ?
                        Mono.error(new EventFullException("The event has reached its maximum capacity."))
                        : Mono.just(event))
                .then(registrationRepository.save(
                                Registration.builder()
                                        .id(UUID.randomUUID())
                                        .userId(user)
                                        .eventId(eventId)
                                        .isNew(true)
                                        .build())
                        .onErrorResume(DuplicateKeyException.class, e ->
                                Mono.error(new DuplicateRegistrationException("User already registered")))
                        .doOnSuccess(r -> log.info("Successfully saved registration: {}", r))
                        .then(eventsRepository.incrementRegistrations(eventId))
                        .doOnSuccess(e -> log.info("Updated event {} registrations to {}", eventId, e.getRegistrations()))
                        .then());
    }

    public Mono<List<EventResponse>> getRegisteredEvents(UUID user) {
        return registrationRepository.findAllByUserId(user)
                .collectList()
                .map(events -> events.stream()
                        .map(Registration::getEventId)
                        .map(eventId -> eventsRepository.findById(eventId)
                                .flatMap(event -> eventCategoryRepository.findAllByEventId(eventId)
                                        .doOnError(ex -> {
                                            throw new RuntimeException("Unable to retrieve event categories for event id: " + eventId);
                                        })
                                        .flatMap(eventCategory -> categoryRepository.findById(eventCategory.getCategoryId()))
                                        .collectList()
                                        .flatMap(categories -> getUserId().map(userId ->
                                                EventResponse.fromEntity(userId, event, categories)))))
                        .toList())
                .map(Flux::concat)
                .flatMap(Flux::collectList)
                .map(eventResponses -> eventResponses.stream()
                        .sorted(Comparator.comparing(EventResponse::getDateTime))
                        .toList());
    }
}
