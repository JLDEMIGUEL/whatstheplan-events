package com.whatstheplan.events.model.email;

import com.whatstheplan.events.model.entities.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEmailData {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private Duration duration;
    private String location;
    private String organizerUsername;

    public static EventEmailData from(Event event, String organizerUsername) {
        return EventEmailData.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .dateTime(event.getDateTime())
                .duration(event.getDuration())
                .location(event.getLocation())
                .organizerUsername(organizerUsername)
                .build();
    }
}
