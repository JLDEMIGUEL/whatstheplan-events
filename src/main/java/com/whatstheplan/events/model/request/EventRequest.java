package com.whatstheplan.events.model.request;

import com.whatstheplan.events.model.entities.Event;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private Duration duration;
    private String location;
    private int capacity;
    private List<String> activityTypes;

    public Mono<Event> toEntity(String imageKey) {
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
                        .build());
    }
}
