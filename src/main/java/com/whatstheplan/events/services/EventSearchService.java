package com.whatstheplan.events.services;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventsRepository eventsRepository;

    public Flux<EventResponse> searchEvents(EventFilterRequest filter) {
        return eventsRepository.searchEvents(filter, "") //TODO get location from user data
                .onErrorResume(e -> {
                    log.error("Error processing event", e);
                    return Flux.empty();
                });
    }
}
