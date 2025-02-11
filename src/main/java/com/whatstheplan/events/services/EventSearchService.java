package com.whatstheplan.events.services;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import com.whatstheplan.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

import static com.whatstheplan.events.utils.RecurrenceUtils.generateRRule;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventsRepository eventsRepository;

    public Flux<EventResponse> searchEvents(EventFilterRequest filter) {
        return eventsRepository.searchEvents(filter)
                .filterWhen(event -> checkEventOccurrence(event, filter))
                .onErrorResume(e -> {
                    log.error("Error processing event", e);
                    return Flux.empty();
                });
    }

    private Mono<Boolean> checkEventOccurrence(EventResponse event, EventFilterRequest eventFilterRequest) {
        return Mono.fromCallable(() -> {
                    if (event.getRecurrence() == null) return true;

                    Recur<LocalDateTime> recurrence = new Recur<>(generateRRule(event.getRecurrence()));

                    return !recurrence.getDates(
                                    event.getDateTime(),
                                    eventFilterRequest.getDateTimeFrom(),
                                    eventFilterRequest.getDateTimeTo())
                            .isEmpty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(false);
    }
}
