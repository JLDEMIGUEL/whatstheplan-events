package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.EventCategories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface EventsCategoriesRepository extends ReactiveCrudRepository<EventCategories, UUID> {
}
