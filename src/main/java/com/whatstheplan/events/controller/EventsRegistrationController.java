package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Event Registration", description = "Register users to events")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/registration")
public class EventsRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    @Operation(
            summary = "Register to an event",
            description = "Registers the authenticated user to the specified event by its UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully registered to the event"),
            @ApiResponse(responseCode = "400", description = "Invalid registration request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "User already registered or event is full", content = @Content)
    })
    @PostMapping("/{eventId}")
    public Mono<ResponseEntity<Void>> registerToEvent(
            @Parameter(description = "UUID of the event to register to", required = true)
            @PathVariable UUID eventId) {
        return getUserId()
                .doOnNext(user -> log.info("Received new registration request from user {} to event: {}", user, eventId))
                .flatMap(user -> eventRegistrationService.register(user, eventId))
                .then(Mono.fromCallable(() -> ResponseEntity.status(HttpStatus.CREATED).build()));
    }

    @Operation(
            summary = "Get events the current user is registered for",
            description = "Retrieves a list of events that the currently authenticated user has registered for."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of registered events returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access - user token missing or invalid",
                    content = @Content
            )
    })
    @GetMapping
    public Mono<ResponseEntity<List<EventResponse>>> getRegisteredEvents() {
        return getUserId()
                .doOnNext(user -> log.info("Received new registered events request from user {}", user))
                .flatMap(eventRegistrationService::getRegisteredEvents)
                .map(ResponseEntity::ok);
    }

}
