package com.whatstheplan.events.controller;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.services.EventSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Event Search", description = "Search for events using filters and pagination")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events/search")
public class EventsSearchController {

    private final EventSearchService eventSearchService;

    @Operation(
            summary = "Search events with filters",
            description = "Searches for events using optional filters such as location, date range, duration, and category."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filtered list of events",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter values or pagination parameters",
                    content = @Content
            )
    })
    @GetMapping
    public Mono<Page<EventResponse>> searchWithFilters(
            @Parameter(description = "Filtering parameters for events")
            @ModelAttribute EventFilterRequest eventFilterRequest,

            @Parameter(in = ParameterIn.QUERY, description = "Page number (0-based)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(in = ParameterIn.QUERY, description = "Page size", example = "10")
            @RequestParam(name = "size", defaultValue = "10") int size) {

        log.info("Received search filter request: {}", eventFilterRequest);
        return eventSearchService.searchEvents(eventFilterRequest, PageRequest.of(page, size));
    }

}
