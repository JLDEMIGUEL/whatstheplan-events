package com.whatstheplan.events.services;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventsRepository eventsRepository;

    public Mono<List<EventResponse>> searchEvents(EventFilterRequest filter) {
        return eventsRepository.searchEvents(filter)
                .collectList()
                .onErrorResume(e -> {
                    log.error("Error processing event", e);
                    return Mono.just(List.of());
                });
    }
}
