package com.whatstheplan.events.services;

import com.whatstheplan.events.exceptions.EventFullException;
import com.whatstheplan.events.model.entities.Registration;
import com.whatstheplan.events.repository.EventsRepository;
import com.whatstheplan.events.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventService eventService;
    private final EventsRepository eventsRepository;
    private final RegistrationRepository registrationRepository;

    public Mono<Void> register(UUID user, UUID eventId) {
        return eventService.findById(eventId)
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
                        .doOnSuccess(r -> log.info("Successfully saved registration: {}", r))
                        .then(eventsRepository.incrementRegistrations(eventId))
                        .doOnSuccess(e -> log.info("Updated event {} registrations to {}", eventId, e.getRegistrations()))
                        .then());
    }
}
