package com.whatstheplan.events.services;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventsRepository eventsRepository;

    public Mono<Page<EventResponse>> searchEvents(EventFilterRequest filter, Pageable pageable) {
        Mono<List<EventResponse>> eventsMono = eventsRepository
                .searchEvents(filter, pageable)
                .collectList();

        Mono<Long> countMono = eventsRepository.countEvents(filter);
        return Mono.zip(eventsMono, countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }
}
