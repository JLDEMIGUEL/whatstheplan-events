package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.EventCategories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventsCategoriesRepository extends ReactiveCrudRepository<EventCategories, UUID> {

    Flux<EventCategories> findAllByEventId(UUID eventId);

    Mono<Void> deleteAllByEventId(UUID eventId);
}
