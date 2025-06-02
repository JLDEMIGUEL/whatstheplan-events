package com.whatstheplan.events.model.request;

import com.whatstheplan.events.model.entities.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.utils.Utils.getUserId;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating an event")
public class EventRequest {

    @NotBlank(message = "Title is required.")
    @Schema(description = "Title of the event", example = "Sunset Yoga in the Park")
    private String title;

    @NotBlank(message = "Description is required.")
    @Schema(description = "Detailed description of the event", example = "Join us for a relaxing yoga session during sunset.")
    private String description;

    @NotNull(message = "Date and time must be specified.")
    @Future(message = "Event date must be in the future.")
    @Schema(description = "Start date and time of the event", example = "2025-07-01T18:00:00")
    private LocalDateTime dateTime;

    @NotNull(message = "Duration is required.")
    @Schema(description = "Duration of the event in ISO-8601 format", example = "PT2H")
    private Duration duration;

    @NotBlank(message = "Location is required.")
    @Schema(description = "Location where the event will take place", example = "Retiro Park, Madrid")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1.")
    @Schema(description = "Maximum number of participants allowed", example = "30")
    private int capacity;

    @Schema(description = "List of activity types associated with the event", example = "[\"yoga\", \"wellness\"]")
    private List<String> activityTypes;

    public Mono<Event> toNewEntity(String imageKey) {
        return getUserId()
                .map(userId -> Event.builder()
                        .id(UUID.randomUUID())
                        .title(title)
                        .description(description)
                        .dateTime(dateTime)
                        .duration(duration)
                        .location(location)
                        .capacity(capacity)
                        .imageKey(imageKey)
                        .organizerId(userId)
                        .isNew(true)
                        .registrations(0)
                        .build());
    }

    public Mono<Event> toUpdateEntity(Event event, String imageKey) {
        return getUserId()
                .map(userId -> Event.builder()
                        .id(event.getId())
                        .title(title)
                        .description(description)
                        .dateTime(dateTime)
                        .duration(duration)
                        .location(location)
                        .capacity(capacity)
                        .imageKey(imageKey)
                        .organizerId(userId)
                        .isNew(false)
                        .registrations(event.getRegistrations())
                        .build());
    }
}
