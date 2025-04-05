package com.whatstheplan.events.model.response;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
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
public class DetailedEventResponse extends EventResponse {
    private Boolean isRegistered;

    public static DetailedEventResponse fromEntityDetailed(UUID userId, Event event, List<Category> activityTypes, Boolean isRegistered) {
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
                .organizerUsername("") //TODO
                .createdDate(event.getCreatedDate())
                .lastModifiedDate(event.getLastModifiedDate())
                .activityTypes(activityTypes.stream().map(Category::getName).toList())
                .registrations(event.getRegistrations())
                .isOwnedByUser(event.getOrganizerId().equals(userId))
                .isRegistered(isRegistered)
                .build();
    }
}

