package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomEventRepository {
    Flux<EventResponse> searchEvents(EventFilterRequest filter, Pageable pageable);

    Mono<Long> countEvents(EventFilterRequest filter);
}
