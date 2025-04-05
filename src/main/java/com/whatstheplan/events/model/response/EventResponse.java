package com.whatstheplan.events.model.response;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private Integer capacity;
    private String imageKey;
    private UUID organizerId;
    private String organizerUsername;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private List<String> activityTypes;
    private Integer registrations;
    private Boolean isOwnedByUser;

    public static EventResponse fromEntity(UUID userId, Event event, List<Category> activityTypes) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .dateTime(event.getDateTime())
                .duration(event.getDuration())
                .location(event.getLocation())
                .capacity(event.getCapacity())
                .imageKey(event.getImageKey())
                .organizerId(event.getOrganizerId())
                .organizerUsername("") //TODO
                .createdDate(event.getCreatedDate())
                .lastModifiedDate(event.getLastModifiedDate())
                .activityTypes(activityTypes.stream().map(Category::getName).toList())
                .registrations(event.getRegistrations())
                .isOwnedByUser(event.getOrganizerId().equals(userId))
                .build();
    }
}

