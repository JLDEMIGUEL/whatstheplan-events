package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.utils.Utils.getUserId;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/registration")
public class EventsRegistrationController {
    private final EventRegistrationService eventRegistrationService;

    @PostMapping("/{eventId}")
    public Mono<ResponseEntity<Void>> registerToEvent(@PathVariable UUID eventId) {
        return getUserId()
                .doOnNext(user -> log.info("Received new registration request from user {} to event: {}", user, eventId))
                .flatMap(user -> eventRegistrationService.register(user, eventId))
                .then(Mono.fromCallable(() -> ResponseEntity.status(HttpStatus.CREATED).build()));
    }

    @GetMapping
    public Mono<ResponseEntity<List<EventResponse>>> getRegisteredEvents() {
        return getUserId()
                .doOnNext(user -> log.info("Received new registered events request from user {}", user))
                .flatMap(eventRegistrationService::getRegisteredEvents)
                .map(ResponseEntity::ok);
    }

}
