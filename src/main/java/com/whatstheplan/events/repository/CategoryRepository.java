package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.Category;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CategoryRepository extends ReactiveCrudRepository<Category, UUID> {

    Mono<Category> findByName(String name);
}
