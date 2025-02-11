package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import reactor.core.publisher.Flux;

public interface CustomEventRepository {
    Flux<EventResponse> searchEvents(EventFilterRequest filter);
}
