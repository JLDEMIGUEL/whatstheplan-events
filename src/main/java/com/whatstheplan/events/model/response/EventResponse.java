package com.whatstheplan.events.model.response;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Detailed response model for an event")
public class EventResponse {

    @Schema(description = "Unique identifier of the event", example = "6fa459ea-ee8a-3ca4-894e-db77e160355e")
    private UUID id;

    @Schema(description = "Title of the event", example = "Summer Festival")
    private String title;

    @Schema(description = "Detailed description of the event", example = "A day full of music, food, and fun.")
    private String description;

    @Schema(description = "Date and time when the event starts", example = "2025-07-15T14:00:00")
    private LocalDateTime dateTime;

    @Schema(description = "Duration of the event in ISO-8601 format", example = "PT5H")
    private Duration duration;

    @Schema(description = "Location where the event takes place", example = "Central Park")
    private String location;

    @Schema(description = "Maximum number of participants allowed", example = "500")
    private Integer capacity;

    @Schema(description = "Storage key or URL for the event image", example = "events/123456/image.png")
    private String imageKey;

    @Schema(description = "UUID of the user who organizes the event", example = "7fa459ea-ee8a-3ca4-894e-db77e160366f")
    private UUID organizerId;

    @Schema(description = "Timestamp when the event was created", example = "2025-04-01T10:15:30Z")
    private Instant createdDate;

    @Schema(description = "Timestamp when the event was last modified", example = "2025-04-10T12:00:00Z")
    private Instant lastModifiedDate;

    @Schema(description = "List of activity types associated with the event", example = "[\"music\", \"food\", \"family\"]")
    private List<String> activityTypes;

    @Schema(description = "Number of users registered for the event", example = "120")
    private Integer registrations;

    @Schema(description = "Whether the event is owned by the current user", example = "true")
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
                .createdDate(event.getCreatedDate())
                .lastModifiedDate(event.getLastModifiedDate())
                .activityTypes(activityTypes.stream().map(Category::getName).toList())
                .registrations(event.getRegistrations())
                .isOwnedByUser(event.getOrganizerId().equals(userId))
                .build();
    }
}
