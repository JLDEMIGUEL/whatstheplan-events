package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/search")
public class EventsSearchController {
    private final EventSearchService eventSearchService;

    @GetMapping
    public Mono<Page<EventResponse>> searchWithFilters(
            @ModelAttribute EventFilterRequest eventFilterRequest,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Received search filter request: {}", eventFilterRequest);
        return eventSearchService.searchEvents(eventFilterRequest, PageRequest.of(page, size));
    }

}
