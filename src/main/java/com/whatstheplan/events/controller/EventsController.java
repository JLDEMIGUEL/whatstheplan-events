package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventsController {

    private final EventService eventService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EventResponse>> createEvent(
            @RequestPart("event") Mono<EventRequest> eventRequestMono,
            @RequestPart("image") Mono<FilePart> imagePartMono
    ) {
        return Mono.zip(eventRequestMono, imagePartMono)
                .doOnSuccess(tuple -> log.info("Received event creation with title \"{}\", and image name \"{}\"",
                        tuple.getT1().getTitle(), tuple.getT2().filename()))
                .flatMap(tuple -> eventService.saveEvent(tuple.getT1(), tuple.getT2()))
                .map(eventResponse -> ResponseEntity.status(HttpStatus.CREATED).body(eventResponse));
    }

}
