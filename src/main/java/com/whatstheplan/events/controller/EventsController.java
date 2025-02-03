package com.whatstheplan.events.controller;

import com.whatstheplan.events.exceptions.FileValidationException;
import com.whatstheplan.events.exceptions.ValidationException;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventService;
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
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventsController {
    private final EventService eventService;
    private final Validator validator;

    @GetMapping("/{eventId}")
    public Mono<ResponseEntity<EventResponse>> getEventById(
            @PathVariable("eventId") UUID eventId) {
        return Mono.just(eventId)
                .flatMap(eventService::findById)
                .map(ResponseEntity::ok);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EventResponse>> createEvent(
            @RequestPart("event") Mono<EventRequest> eventRequestMono,
            @RequestPart("image") Mono<FilePart> imagePartMono) {
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EventResponse>> updateEvent(
            @PathVariable("id") UUID eventId,
            @RequestPart("event") Mono<EventRequest> eventRequestMono,
            @RequestPart(name = "image", required = false) Mono<FilePart> imagePartMono) {
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

    @DeleteMapping("/{eventId}")
    public Mono<ResponseEntity<EventResponse>> deleteEventById(
            @PathVariable("eventId") UUID eventId) {
        return Mono.just(eventId)
                .flatMap(eventService::deleteById)
                .thenReturn(ResponseEntity.ok().build());
    }

    private void validateEventRequest(EventRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "eventRequest");
        validator.validate(request, errors);

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
        String filename = image.filename();
        if (!filename.matches("(?i).*\\.(png|jpg|jpeg)$")) {
            throw new FileValidationException("Invalid image format. Allowed: PNG, JPG, JPEG.");
        }
        if (image.headers().getContentLength() > 5 * 1024 * 1024) {
            throw new FileValidationException("Image size exceeds 5MB.");
        }
    }
}
