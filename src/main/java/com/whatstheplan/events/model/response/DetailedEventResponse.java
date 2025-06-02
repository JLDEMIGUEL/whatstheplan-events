package com.whatstheplan.events.model.response;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Detailed event response including registration status and organizer info")
public class DetailedEventResponse extends EventResponse {

    @Schema(description = "Indicates if the current user is registered for the event", example = "true")
    private Boolean isRegistered;

    @Schema(description = "Username of the event organizer", example = "organizer_jane")
    private String organizerUsername;

    public static DetailedEventResponse fromEntityDetailed(UUID userId, Event event, List<Category> activityTypes, Boolean isRegistered, String organizerUsername) {
        return DetailedEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .dateTime(event.getDateTime())
                .duration(event.getDuration())
                .location(event.getLocation())
                .capacity(event.getCapacity())
                .imageKey(event.getImageKey())
                .organizerId(event.getOrganizerId())
                .organizerUsername(organizerUsername)
                .createdDate(event.getCreatedDate())
                .lastModifiedDate(event.getLastModifiedDate())
                .activityTypes(activityTypes.stream().map(Category::getName).toList())
                .registrations(event.getRegistrations())
                .isOwnedByUser(event.getOrganizerId().equals(userId))
                .isRegistered(isRegistered)
                .build();
    }
}

