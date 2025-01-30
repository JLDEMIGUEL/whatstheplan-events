package com.whatstheplan.events.model.request;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Event;
import jakarta.validation.Valid;
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

import static com.whatstheplan.events.utils.RecurrenceUtils.generateRRule;
import static com.whatstheplan.events.utils.Utils.getUserId;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    @NotBlank(message = "Title is required.")
    private String title;

    @NotBlank(message = "Description is required.")
    private String description;

    @NotNull(message = "Date and time must be specified.")
    @Future(message = "Event date must be in the future.")
    private LocalDateTime dateTime;

    @NotNull(message = "Duration is required.")
    private Duration duration;

    @NotBlank(message = "Location is required.")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1.")
    private int capacity;

    @Valid
    private Recurrence recurrence;

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
                        .recurrence(generateRRule(recurrence))
                        .organizerId(userId)
                        .isNew(true)
                        .build());
    }
}
