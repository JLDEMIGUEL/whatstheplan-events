package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.entities.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventsRepository extends ReactiveCrudRepository<Event, UUID> {

    @Query("""
            INSERT INTO event (
                id, 
                title, 
                description, 
                date_time, 
                duration, 
                location, 
                capacity, 
                image_key, 
                is_recurring, 
                recurrence_pattern, 
                organizer_id, 
                created_date, 
                last_modified_date
            ) VALUES (
                :#{#event.id}, 
                :#{#event.title}, 
                :#{#event.description}, 
                :#{#event.dateTime}, 
                CAST(:#{#event.duration} AS INTERVAL), 
                :#{#event.location}, 
                :#{#event.capacity}, 
                :#{#event.imageKey}, 
                :#{#event.isRecurring}, 
                :#{#event.recurrencePattern}, 
                :#{#event.organizerId}, 
                :#{#event.createdDate}, 
                :#{#event.lastModifiedDate}
            )
            RETURNING *
            """)
    Mono<Event> insert(Event event);
}
