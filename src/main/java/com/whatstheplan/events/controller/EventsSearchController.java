package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/search")
public class EventsSearchController {
    private final EventSearchService eventSearchService;

    @GetMapping
    public Mono<ResponseEntity<List<EventResponse>>> searchWithFilters(@ModelAttribute EventFilterRequest eventFilterRequest) {
        return Mono.just(eventFilterRequest)
                .doOnNext(request -> log.info("Received search filter request: {}", request))
                .flatMap(request -> eventSearchService.searchEvents(request)
                        .collectList()
                        .map(ResponseEntity::ok));
    }

}
