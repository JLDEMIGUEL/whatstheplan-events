package com.whatstheplan.events.repository;

import com.whatstheplan.events.model.request.EventFilterRequest;
import com.whatstheplan.events.model.response.EventResponse;
import io.r2dbc.postgresql.codec.Interval;
import io.r2dbc.spi.Parameters;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.whatstheplan.events.utils.RecurrenceUtils.parseRRule;

public class CustomEventRepositoryImpl implements CustomEventRepository {
    private final DatabaseClient databaseClient;

    public CustomEventRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<EventResponse> searchEvents(EventFilterRequest filter) {
        StringBuilder sql = new StringBuilder("""
                    SELECT
                        e.*,
                        COALESCE(array_agg(c.name) FILTER (WHERE c.name IS NOT NULL), '{}'::VARCHAR[]) AS activity_types 
                    FROM event e 
                    LEFT JOIN event_categories ec ON e.id = ec.event_id 
                    LEFT JOIN category c ON ec.category_id = c.id 
                    WHERE 1=1
                """);

        Map<String, Object> params = new HashMap<>();

        if (filter.getLocation() != null && !filter.getLocation().isEmpty()) {
            sql.append(" AND e.location = :location");
            params.put("location", filter.getLocation());
        }
        if (filter.getDurationFrom() != null) {
            sql.append(" AND e.duration >= :durationFrom");
            params.put("durationFrom", Interval.from(filter.getDurationFrom()));
        }
        if (filter.getDurationTo() != null) {
            sql.append(" AND e.duration <= :durationTo");
            params.put("durationTo", Interval.from(filter.getDurationTo()));
        }
        if (filter.getCapacityMin() != null) {
            sql.append(" AND e.capacity >= :capacityMin");
            params.put("capacityMin", filter.getCapacityMin());
        }
        if (filter.getCapacityMax() != null) {
            sql.append(" AND e.capacity <= :capacityMax");
            params.put("capacityMax", filter.getCapacityMax());
        }
        if (filter.getActivityTypes() != null && !filter.getActivityTypes().isEmpty()) {
            sql.append(" AND e.id IN (SELECT ec.event_id FROM event_categories ec " +
                    "JOIN category c ON ec.category_id = c.id WHERE c.name IN (:categories))");
            params.put("categories", Parameters.in(filter.getActivityTypes()));
        }

        sql.append(" AND (e.recurrence IS NOT NULL OR e.date_time >= :after)");
        params.put("after", filter.getDateTimeFrom());
        sql.append(" AND (e.recurrence IS NOT NULL OR e.date_time <= :before)");
        params.put("before", filter.getDateTimeTo());

        sql.append(" GROUP BY e.id");
        sql.append(" ORDER BY e.date_time ASC");

        return databaseClient.sql(sql.toString())
                .bindValues(params)
                .map((row, rowMetadata) -> {
                    EventResponse response = EventResponse.builder()
                            .id(row.get("id", UUID.class))
                            .title(row.get("title", String.class))
                            .description(row.get("description", String.class))
                            .dateTime(row.get("date_time", LocalDateTime.class))
                            .duration(Objects.requireNonNull(row.get("duration", Interval.class)).getDuration())
                            .location(row.get("location", String.class))
                            .capacity(row.get("capacity", Integer.class))
                            .imageKey(row.get("image_key", String.class))
                            .organizerId(row.get("organizer_id", UUID.class))
                            .recurrence(parseRRule(row.get("recurrence", String.class)))
                            .createdDate(row.get("created_date", Instant.class))
                            .lastModifiedDate(row.get("last_modified_date", Instant.class))
                            .build();

                    String[] activityTypesArray = row.get("activity_types", String[].class);
                    List<String> activityTypesList = activityTypesArray != null
                            ? Arrays.asList(activityTypesArray)
                            : Collections.emptyList();
                    response.setActivityTypes(activityTypesList);

                    return response;
                })
                .all();
    }
}
