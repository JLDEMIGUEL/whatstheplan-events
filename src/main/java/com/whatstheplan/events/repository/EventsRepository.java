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
                recurrence, 
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
                :#{#event.recurrence}, 
                :#{#event.organizerId}, 
                :#{#event.createdDate}, 
                :#{#event.lastModifiedDate}
            )
            RETURNING *
            """)
    Mono<Event> insert(Event event);

    @Query("""
            UPDATE event
            SET
                title = :#{#event.title},
                description = :#{#event.description},
                date_time = :#{#event.dateTime},
                duration = CAST(:#{#event.duration} AS INTERVAL),
                location = :#{#event.location},
                capacity = :#{#event.capacity},
                image_key = :#{#event.imageKey},
                recurrence = :#{#event.recurrence},
                organizer_id = :#{#event.organizerId},
                last_modified_date = :#{#event.lastModifiedDate}
            WHERE id = :#{#event.id}
            RETURNING *
            """)
    Mono<Event> update(Event event);
}
