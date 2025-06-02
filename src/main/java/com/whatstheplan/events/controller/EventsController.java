package com.whatstheplan.events.controller;

import com.whatstheplan.events.exceptions.FileValidationException;
import com.whatstheplan.events.exceptions.ValidationException;
import com.whatstheplan.events.model.ActivityType;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.DetailedEventResponse;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.whatstheplan.events.utils.Utils.getUserId;

@Tag(name = "Events", description = "Operations related to user-created events")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventsController {

    private final EventService eventService;
    private final Validator validator;

    @Operation(summary = "Get event by ID", description = "Returns detailed information about an event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful retrieval of event",
                    content = @Content(schema = @Schema(implementation = DetailedEventResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{eventId}")
    public Mono<ResponseEntity<DetailedEventResponse>> getEventById(
            @Parameter(description = "UUID of the event to retrieve", required = true)
            @PathVariable("eventId") UUID eventId) {
        return Mono.just(eventId)
                .flatMap(eventService::findById)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Get events created or registered by current user")
    @ApiResponse(responseCode = "200", description = "List of events",
            content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @GetMapping
    public Mono<ResponseEntity<List<EventResponse>>> getUserEvents() {
        return getUserId()
                .map(eventService::findByUserId)
                .flatMap(Flux::collectList)
                .doOnSuccess(response -> log.info("Returning event responses: {}", response))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Create a new event",
            description = "Creates an event with metadata and an image. Requires multipart/form-data.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EventResponse>> createEvent(
            @Parameter(description = "Event metadata") @RequestPart("event") Mono<EventRequest> eventRequestMono,
            @Parameter(description = "Image file for the event") @RequestPart("image") Mono<FilePart> imagePartMono) {
        Mono<EventRequest> validatedEvent = eventRequestMono
                .doOnNext(this::validateEventRequest)
                .onErrorMap(ValidationException.class, Function.identity());

        Mono<FilePart> validatedImage = imagePartMono
                .doOnNext(this::validateImage)
                .onErrorMap(FileValidationException.class, Function.identity());

        return Mono.zip(validatedEvent, validatedImage)
                .flatMap(tuple -> eventService.saveEvent(tuple.getT1(), tuple.getT2()))
                .map(event -> ResponseEntity.status(HttpStatus.CREATED).body(event));
    }

    @Operation(summary = "Update an existing event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EventResponse>> updateEvent(
            @Parameter(description = "UUID of the event to update", required = true)
            @PathVariable("id") UUID eventId,
            @Parameter(description = "Updated event metadata") @RequestPart("event") Mono<EventRequest> eventRequestMono,
            @Parameter(description = "Optional updated image") @RequestPart(name = "image", required = false) Mono<FilePart> imagePartMono) {
        Mono<EventRequest> validatedEvent = eventRequestMono
                .doOnNext(this::validateEventRequest)
                .onErrorMap(ValidationException.class, Function.identity());

        Mono<Optional<FilePart>> validatedImage = imagePartMono
                .map(Optional::of)
                .switchIfEmpty(Mono.just(Optional.empty()))
                .doOnNext(this::validateImageUpdate)
                .onErrorMap(FileValidationException.class, Function.identity());

        return Mono.zip(Mono.just(eventId), validatedEvent, validatedImage)
                .flatMap(t -> eventService.updateEvent(t.getT1(), t.getT2(), t.getT3()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Delete an event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{eventId}")
    public Mono<ResponseEntity<EventResponse>> deleteEventById(
            @Parameter(description = "UUID of the event to delete", required = true)
            @PathVariable("eventId") UUID eventId) {
        return Mono.just(eventId)
                .flatMap(eventService::deleteById)
                .thenReturn(ResponseEntity.ok().build());
    }

    private void validateEventRequest(EventRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "eventRequest");
        validator.validate(request, errors);

        Optional.ofNullable(request.getActivityTypes()).orElse(List.of()).forEach(ActivityType::from);

        if (errors.hasErrors()) {
            throw new ValidationException(String.join(" ", errors.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .toList()));
        }
    }

    private void validateImageUpdate(Optional<FilePart> image) {
        if (image.isEmpty()) {
            return;
        }
        validateImage(image.get());
    }

    private void validateImage(FilePart image) {
        if (!image.filename().matches("(?i).*\\.(png|jpg|jpeg)$")) {
            throw new FileValidationException("Invalid image format. Allowed: PNG, JPG, JPEG.");
        }
        if (image.headers().getContentLength() > 5 * 1024 * 1024) {
            throw new FileValidationException("Image size exceeds 5MB.");
        }
    }
}
