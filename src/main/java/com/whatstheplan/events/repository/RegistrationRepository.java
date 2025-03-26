package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.Registration;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RegistrationRepository extends ReactiveCrudRepository<Registration, UUID> {

    Mono<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);

    Flux<Registration> findAllByUserId(UUID userId);
}
