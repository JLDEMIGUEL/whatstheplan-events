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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventsController {
    private final EventService eventService;
    private final Validator validator;

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

    private void validateEventRequest(EventRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "eventRequest");
        validator.validate(request, errors);

        if (errors.hasErrors()) {
            throw new ValidationException(String.join(" ", errors.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .toList()));
        }
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
