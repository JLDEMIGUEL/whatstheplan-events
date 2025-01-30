package com.whatstheplan.events.model.response;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.utils.RecurrenceUtils.parseRRule;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private Duration duration;
    private String location;
    private int capacity;
    private String imageKey;
    private Recurrence recurrence;
    private UUID organizerId;
    private String organizerUsername;
    private String organizerEmail;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private List<String> activityTypes;

    public static EventResponse fromEntity(Event event, List<EventCategories> activityTypes) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .dateTime(event.getDateTime())
                .duration(event.getDuration())
                .location(event.getLocation())
                .capacity(event.getCapacity())
                .imageKey(event.getImageKey())
                .recurrence(parseRRule(event.getRecurrence()))
                .organizerId(event.getOrganizerId())
                .organizerEmail("") //TODO
                .organizerUsername("") //TODO
                .createdDate(event.getCreatedDate())
                .lastModifiedDate(event.getLastModifiedDate())
                .activityTypes(activityTypes.stream().map(EventCategories::getActivityType).toList())
                .build();
    }
}

