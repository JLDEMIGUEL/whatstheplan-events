package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.Registration;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface RegistrationRepository extends ReactiveCrudRepository<Registration, UUID> {

    Flux<Registration> findAllByUserId(UUID userId);
}
